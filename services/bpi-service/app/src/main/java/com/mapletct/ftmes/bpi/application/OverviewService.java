package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiNotFoundException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.LineEvidenceCheck;
import com.mapletct.ftmes.bpi.domain.LineIncidentSnapshot;
import com.mapletct.ftmes.bpi.domain.LineLiveEvidence;
import com.mapletct.ftmes.bpi.domain.LineState;
import com.mapletct.ftmes.bpi.domain.LineTelemetrySample;
import com.mapletct.ftmes.bpi.domain.LineTelemetryState;
import com.mapletct.ftmes.bpi.infrastructure.overview.BpiOverviewProperties;
import com.mapletct.ftmes.bpi.infrastructure.postgres.OverviewPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.OverviewPostgresRepository.LineProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OverviewService {
    private static final int MAX_WINDOW_MINUTES = 1_440;
    private static final int MAX_TREND_LIMIT = 500;

    private final OverviewPostgresRepository repository;
    private final BpiOverviewProperties properties;
    private final Clock clock;

    @Autowired
    public OverviewService(
            OverviewPostgresRepository repository,
            BpiOverviewProperties properties) {
        this(repository, properties, Clock.systemUTC());
    }

    OverviewService(
            OverviewPostgresRepository repository,
            BpiOverviewProperties properties,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LineState> overview(ActorContext actor, String plantId, boolean onlyAbnormal) {
        String normalizedPlant = required(plantId, "plantId", 64);
        assertPlantAccess(actor, normalizedPlant);
        Instant now = clock.instant();
        return repository.listLines(
                        actor,
                        normalizedPlant,
                        null,
                        now.minus(properties.telemetryFreshness()))
                .stream()
                .map(line -> state(line, now))
                .filter(line -> !onlyAbnormal || abnormal(line))
                .sorted(Comparator.comparing(LineState::lineId))
                .toList();
    }

    @Transactional(readOnly = true)
    public LineState current(ActorContext actor, String plantId, String lineId) {
        Instant now = clock.instant();
        return state(find(actor, plantId, lineId, now), now);
    }

    @Transactional(readOnly = true)
    public LineLiveEvidence liveEvidence(
            ActorContext actor,
            String plantId,
            String lineId,
            Integer windowMinutes,
            Integer limit) {
        int normalizedWindow = windowMinutes == null
                ? properties.defaultTrendWindowMinutes()
                : windowMinutes;
        int normalizedLimit = limit == null ? properties.defaultTrendLimit() : limit;
        if (normalizedWindow < 1 || normalizedWindow > MAX_WINDOW_MINUTES) {
            throw new BpiValidationException("windowMinutes must be between 1 and 1440.");
        }
        if (normalizedLimit < 2 || normalizedLimit > MAX_TREND_LIMIT) {
            throw new BpiValidationException("limit must be between 2 and 500.");
        }

        Instant now = clock.instant();
        LineProjection projection = find(actor, plantId, lineId, now);
        LineState line = state(projection, now);
        Instant windowStart = now.minus(Duration.ofMinutes(normalizedWindow));
        List<LineTelemetrySample> samples = repository.listSamples(
                actor, projection, windowStart, now, normalizedLimit);
        List<LineIncidentSnapshot> incidents = repository.listOpenIncidents(
                actor, projection.plantId(), projection.lineId(), 20);
        return new LineLiveEvidence(
                line,
                windowStart,
                now,
                samples,
                checks(line),
                incidents);
    }

    private LineProjection find(
            ActorContext actor,
            String plantId,
            String lineId,
            Instant now) {
        String normalizedLine = required(lineId, "lineId", 128);
        String normalizedPlant = plantId == null || plantId.isBlank()
                ? null
                : required(plantId, "plantId", 64);
        if (normalizedPlant != null) {
            if (!actor.canAccess(normalizedPlant, normalizedLine)) {
                throw new BpiForbiddenException("Token scope does not allow this BPI line.");
            }
        } else if (!actor.lineIds().contains("*") && !actor.lineIds().contains(normalizedLine)) {
            throw new BpiForbiddenException("Token scope does not allow this BPI line.");
        }

        List<LineProjection> matches = repository.listLines(
                actor,
                normalizedPlant,
                normalizedLine,
                now.minus(properties.telemetryFreshness()));
        if (matches.isEmpty()) {
            throw new BpiNotFoundException("Line has no BPI context.");
        }
        if (normalizedPlant == null && matches.size() > 1) {
            throw new BpiValidationException(
                    "plantId is required because the line exists in more than one plant.");
        }
        return matches.get(0);
    }

    private LineState state(LineProjection line, Instant now) {
        long lagSeconds = line.sampleTime() == null
                ? 0
                : Math.max(0, Duration.between(line.sampleTime(), now).getSeconds());
        boolean fresh = line.sampleTime() != null
                && !line.sampleTime().isBefore(now.minus(properties.telemetryFreshness()));
        String dataHealth = dataHealth(line, fresh);
        String status = status(line, dataHealth);
        Instant lastEvent = latest(
                line.sampleTime(),
                line.batchStartTime(),
                line.candidateTime());
        String unit = line.actualUnit() == null ? line.expectedUnit() : line.actualUnit();
        LineTelemetryState telemetry = new LineTelemetryState(
                line.topologyBound(),
                line.primarySignal(),
                line.primaryProductId(),
                line.primaryDeviceId(),
                line.primaryPropertyId(),
                line.value(),
                line.numericValue(),
                unit,
                line.qualityCode(),
                line.sequenceOrigin(),
                line.sequenceDisposition(),
                line.sampleTime(),
                line.calibrationVersion(),
                lagSeconds,
                fresh,
                line.expectedSignalCount(),
                line.observedSignalCount(),
                line.goodSignalCount(),
                line.openIncidentCount(),
                line.criticalIncidentCount());
        return new LineState(
                line.plantId(),
                line.lineId(),
                line.lineId(),
                line.batchOrderId() == null ? line.candidateOrderId() : line.batchOrderId(),
                line.currentBatchId(),
                line.stageCode() == null ? "UNASSIGNED" : line.stageCode(),
                status,
                line.confidence(),
                line.numericValue(),
                line.totalizedQuantity() == null ? BigDecimal.ZERO : line.totalizedQuantity(),
                dataHealth,
                line.pendingCandidates(),
                line.openIncidentCount(),
                lastEvent,
                telemetry);
    }

    private String dataHealth(LineProjection line, boolean fresh) {
        if (line.errorIncidentCount() > 0
                || "BAD".equals(line.qualityCode())
                || "STALE".equals(line.qualityCode())
                || "GAP".equals(line.sequenceDisposition())
                || "OUT_OF_ORDER".equals(line.sequenceDisposition())) {
            return "BAD";
        }
        if (!line.topologyBound()
                || line.sampleTime() == null
                || !fresh
                || line.expectedSignalCount() <= 0
                || line.observedSignalCount() < line.expectedSignalCount()
                || line.goodSignalCount() < line.expectedSignalCount()
                || line.openIncidentCount() > 0
                || "UNCERTAIN".equals(line.qualityCode())
                || "SUBSTITUTED".equals(line.qualityCode())) {
            return "PARTIAL";
        }
        return "GOOD";
    }

    private String status(LineProjection line, String dataHealth) {
        if ("SUSPENDED".equals(line.batchState()) || "BAD".equals(dataHealth)) {
            return "BLOCKED";
        }
        if ("PARTIAL".equals(dataHealth)) {
            return "DEGRADED";
        }
        return line.currentBatchId() == null ? "IDLE" : "RUNNING";
    }

    private boolean abnormal(LineState line) {
        return line.pendingCandidates() > 0
                || !"GOOD".equals(line.dataHealth())
                || "BLOCKED".equals(line.status())
                || "DEGRADED".equals(line.status());
    }

    private List<LineEvidenceCheck> checks(LineState line) {
        LineTelemetryState telemetry = line.telemetry();
        List<LineEvidenceCheck> checks = new ArrayList<>();
        checks.add(check(
                "TOPOLOGY_BOUND",
                "已发布拓扑绑定关键工艺信号",
                telemetry.topologyBound() ? "PASS" : "FAIL",
                telemetry.topologyBound()
                        ? telemetry.primarySignal() + " -> "
                            + telemetry.productId() + "/" + telemetry.deviceId()
                            + "/" + telemetry.propertyId()
                        : "当前产线没有可用的已发布拓扑 binding"));
        checks.add(check(
                "TELEMETRY_FRESH",
                "遥测仍在允许时效内",
                telemetry.fresh() ? "PASS" : "FAIL",
                telemetry.sampleTime() == null
                        ? "尚未收到真实遥测"
                        : "最后样本延迟 " + telemetry.lagSeconds() + " 秒"));
        boolean observed = telemetry.expectedSignalCount() > 0
                && telemetry.observedSignalCount() >= telemetry.expectedSignalCount();
        checks.add(check(
                "REQUIRED_SIGNALS_OBSERVED",
                "必需点位全部到达",
                observed ? "PASS" : "FAIL",
                telemetry.observedSignalCount() + "/" + telemetry.expectedSignalCount()));
        boolean good = telemetry.expectedSignalCount() > 0
                && telemetry.goodSignalCount() >= telemetry.expectedSignalCount();
        checks.add(check(
                "REQUIRED_SIGNALS_GOOD",
                "必需点位质量为 GOOD",
                good ? "PASS" : "FAIL",
                telemetry.goodSignalCount() + "/" + telemetry.expectedSignalCount()));
        boolean sequencePresent = telemetry.sequenceDisposition() != null;
        boolean sequenceBad = "GAP".equals(telemetry.sequenceDisposition())
                || "OUT_OF_ORDER".equals(telemetry.sequenceDisposition());
        checks.add(check(
                "SOURCE_SEQUENCE_CONTINUOUS",
                "来源序列连续",
                sequenceBad ? "FAIL" : sequencePresent ? "PASS" : "WARN",
                sequencePresent
                        ? telemetry.sequenceOrigin() + " / " + telemetry.sequenceDisposition()
                        : "尚无来源序列事实"));
        checks.add(check(
                "NO_UNRESOLVED_CRITICAL_INCIDENT",
                "无未解决严重数据质量事件",
                telemetry.criticalIncidentCount() == 0 ? "PASS" : "FAIL",
                telemetry.criticalIncidentCount() + " 个 CRITICAL，"
                        + telemetry.openIncidentCount() + " 个未解决事件"));
        boolean context = line.currentBatchId() != null || line.orderId() != null;
        checks.add(check(
                "PRODUCTION_CONTEXT_AVAILABLE",
                "MES 生产上下文可用",
                context ? "PASS" : "WARN",
                context ? "生产指令 " + line.orderId() : "当前未关联生产指令或活动批次"));
        return List.copyOf(checks);
    }

    private LineEvidenceCheck check(
            String code,
            String label,
            String status,
            String detail) {
        return new LineEvidenceCheck(code, label, status, detail);
    }

    private Instant latest(Instant... values) {
        Instant result = null;
        for (Instant value : values) {
            if (value != null && (result == null || value.isAfter(result))) {
                result = value;
            }
        }
        return result == null ? Instant.EPOCH : result;
    }

    private void assertPlantAccess(ActorContext actor, String plantId) {
        if (!actor.plantIds().contains("*") && !actor.plantIds().contains(plantId)) {
            throw new BpiForbiddenException("Token scope does not allow this BPI plant.");
        }
    }

    private String required(String value, String field, int maximumLength) {
        if (value == null || value.isBlank() || value.length() > maximumLength) {
            throw new BpiValidationException(
                    field + " is required and must not exceed " + maximumLength + " characters.");
        }
        return value.trim();
    }
}
