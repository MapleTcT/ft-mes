package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DataQualityIncidentDetail;
import com.mapletct.ftmes.bpi.domain.DataQualityIncidentView;
import com.mapletct.ftmes.bpi.domain.DataQualitySummary;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DataQualityPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.DataQualityAcknowledgeCommand;
import com.mapletct.ftmes.bpi.interfaces.rest.ReasonCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataQualityIncidentService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final Set<String> STATES = Set.of("OPEN", "ACKNOWLEDGED", "RESOLVED");
    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 200;

    private final DataQualityPostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final DataQualityIncidentCursorCodec cursorCodec;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public DataQualityIncidentService(
            DataQualityPostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            DataQualityIncidentCursorCodec cursorCodec,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.cursorCodec = cursorCodec;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DataQualityIncidentPage list(
            ActorContext actor,
            String plantId,
            String lineId,
            String requestedState,
            String requestedSearch,
            String encodedCursor,
            Integer requestedLimit) {
        assertScope(actor, plantId, lineId);
        String state = normalizedState(requestedState);
        String search = normalizedSearch(requestedSearch);
        int limit = pageSize(requestedLimit);
        String fingerprint = scopeFingerprint(actor, plantId, lineId, state, search);
        DataQualityIncidentCursorCodec.Cursor cursor = encodedCursor == null || encodedCursor.isBlank()
                ? null : cursorCodec.decode(encodedCursor, fingerprint);
        Instant snapshotAt = cursor == null ? repository.currentTransactionTime() : cursor.snapshotAt();
        List<DataQualityIncidentView> values = repository.list(
                actor, plantId, lineId, state, search, snapshotAt, cursor, limit + 1);
        boolean hasMore = values.size() > limit;
        List<DataQualityIncidentView> items = hasMore ? values.subList(0, limit) : values;
        String nextCursor = null;
        if (hasMore) {
            DataQualityIncidentView last = items.get(items.size() - 1);
            nextCursor = cursorCodec.encode(new DataQualityIncidentCursorCodec.Cursor(
                    snapshotAt, last.affectedBatchCount(), severityRank(last.severity()),
                    last.lastSeen(), last.id()), fingerprint);
        }
        return new DataQualityIncidentPage(items, snapshotAt, nextCursor);
    }

    @Transactional(readOnly = true)
    public DataQualitySummary summary(ActorContext actor, String plantId, String lineId) {
        assertScope(actor, plantId, lineId);
        return repository.summary(actor, plantId, lineId);
    }

    @Transactional(readOnly = true)
    public DataQualityIncidentDetail detail(ActorContext actor, UUID incidentId) {
        DataQualityIncidentView incident = repository.find(actor, incidentId);
        return new DataQualityIncidentDetail(
                incident,
                repository.events(actor, incidentId, 100),
                repository.lifecycle(actor, incidentId),
                recommendedActions(incident.issueCode()));
    }

    @Transactional(timeout = 15)
    public CommandResult<DataQualityIncidentView> acknowledge(
            ActorContext actor,
            UUID incidentId,
            String idempotencyKey,
            String ifMatch,
            DataQualityAcknowledgeCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String assignee = normalizedAssignee(command.assignee());
        String reason = normalizedReason(command.reason());
        repository.find(actor, incidentId);
        String path = "/bpi/v1/data-quality/incidents/" + incidentId + "/acknowledge";
        String checksum = Checksums.sha256(
                incidentId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DataQualityIncidentView> replay = replay(actor, idempotencyKey, path, checksum);
        if (replay != null) return replay;
        repository.acknowledge(actor, incidentId, expectedRevision, assignee, reason, traceId);
        DataQualityIncidentView value = repository.find(actor, incidentId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(value));
        return new CommandResult<>(value, false);
    }

    @Transactional(timeout = 15)
    public CommandResult<DataQualityIncidentView> resolve(
            ActorContext actor,
            UUID incidentId,
            String idempotencyKey,
            String ifMatch,
            ReasonCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String reason = normalizedReason(command.reason());
        repository.find(actor, incidentId);
        String path = "/bpi/v1/data-quality/incidents/" + incidentId + "/resolve";
        String checksum = Checksums.sha256(
                incidentId + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<DataQualityIncidentView> replay = replay(actor, idempotencyKey, path, checksum);
        if (replay != null) return replay;
        repository.resolve(actor, incidentId, expectedRevision, reason, traceId);
        DataQualityIncidentView value = repository.find(actor, incidentId);
        sharedRepository.completeIdempotency(actor.tenantId(), idempotencyKey, 200, writeJson(value));
        return new CommandResult<>(value, false);
    }

    private CommandResult<DataQualityIncidentView> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (owner) return null;
        IdempotencyRecord previous = sharedRepository.lockIdempotency(actor.tenantId(), idempotencyKey);
        if (!"POST".equals(previous.method()) || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException("Idempotency-Key was reused with a different request.", null);
        }
        if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
            return new CommandResult<>(sharedRepository.readJson(
                    previous.responseBody(), new TypeReference<DataQualityIncidentView>() {}), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void assertScope(ActorContext actor, String plantId, String lineId) {
        if (plantId == null || plantId.isBlank()) {
            throw new BpiValidationException("plantId is required for data-quality access.");
        }
        if (!actor.plantIds().contains("*") && !actor.plantIds().contains(plantId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested plant.");
        }
        if (lineId != null && !lineId.isBlank() && !actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow the requested data-quality line.");
        }
    }

    private String normalizedState(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toUpperCase();
        if (!STATES.contains(normalized)) {
            throw new BpiValidationException("state must be OPEN, ACKNOWLEDGED or RESOLVED.");
        }
        return normalized;
    }

    private String normalizedSearch(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 200) {
            throw new BpiValidationException("search must not exceed 200 characters.");
        }
        return normalized;
    }

    private int pageSize(Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_PAGE_SIZE : requestedLimit;
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new BpiValidationException("limit must be between 1 and 200.");
        }
        return limit;
    }

    private String scopeFingerprint(
            ActorContext actor,
            String plantId,
            String lineId,
            String state,
            String search) {
        List<String> lines = new ArrayList<>(actor.lineIds());
        lines.sort(Comparator.naturalOrder());
        return Checksums.sha256(String.join("|",
                part(actor.tenantId()), part(plantId), part(value(lineId)), part(value(state)),
                part(value(search)), part(String.join(",", lines))));
    }

    private List<String> recommendedActions(String issueCode) {
        String code = issueCode.toUpperCase();
        if (code.contains("CLOCK") || code.contains("TIME")) {
            return List.of("核对数据源、Kafka broker 与 Flink 节点的 NTP 状态。", "确认事件时间字段和时区转换未发生二次换算。", "校时后观察至少一个完整批次窗口。");
        }
        if (code.contains("UNIT")) {
            return List.of("核对点位目录中的工程单位和 BPI 单位别名。", "确认源系统没有在同一 propertyId 下切换单位。", "修复后重新执行规则回放。");
        }
        if (code.contains("SEQUENCE") || code.contains("GAP")) {
            return List.of("检查源事件序列号、Kafka 分区和生产者重试策略。", "按 sourceEventId 核对缺失范围，避免用插值替代批次边界证据。", "从最后完整 checkpoint 开始受控重放。");
        }
        if (code.contains("SHARED") || code.contains("ALLOCAT")) {
            return List.of("为共享计量点配置可审计的分摊键和有效期。", "确认同一时间窗口内各产线分摊比例之和为 100%。", "重新计算受影响批次的物料平衡。");
        }
        if (code.contains("BUFFER") || code.contains("BACKPRESSURE")) {
            return List.of("检查 Flink backpressure、checkpoint 时长和 Kafka consumer lag。", "确认缓冲区未跨越允许的最大乱序时间。", "容量恢复后核对迟到事件是否重新打开事件。");
        }
        return List.of("检查点位在线状态、质量码和最近原始事件。", "核对该点位关联的规则与受影响批次。", "修复后观察完整生产窗口再解决事件。");
    }

    private String normalizedAssignee(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 128) {
            throw new BpiValidationException("assignee must contain between 1 and 128 non-whitespace characters.");
        }
        return normalized;
    }

    private String normalizedReason(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < 3 || normalized.length() > 500) {
            throw new BpiValidationException("reason must contain between 3 and 500 non-whitespace characters.");
        }
        return normalized;
    }

    private void validateHeaders(String idempotencyKey, String ifMatch) {
        if (idempotencyKey == null || idempotencyKey.length() < 8 || ifMatch == null) {
            throw new BpiPreconditionRequiredException("Idempotency-Key and If-Match are required.");
        }
        if (idempotencyKey.length() > 128) {
            throw new BpiValidationException("Idempotency-Key must not exceed 128 characters.");
        }
        parseRevision(ifMatch);
    }

    private long parseRevision(String ifMatch) {
        Matcher matcher = REVISION_HEADER.matcher(ifMatch == null ? "" : ifMatch.trim());
        if (!matcher.matches()) {
            throw new BpiPreconditionRequiredException("If-Match must contain a numeric entity revision.");
        }
        return Long.parseLong(matcher.group(1));
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "ERROR" -> 3;
            case "WARNING" -> 2;
            default -> 1;
        };
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String part(String value) {
        return value.length() + ":" + value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize BPI data-quality command", exception);
        }
    }
}
