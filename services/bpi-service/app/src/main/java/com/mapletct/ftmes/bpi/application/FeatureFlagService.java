package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.application.error.BpiPreconditionRequiredException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.FeatureFlagRecord;
import com.mapletct.ftmes.bpi.domain.FeatureFlagView;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.FeatureFlagPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.IdempotencyRecord;
import com.mapletct.ftmes.bpi.interfaces.rest.FeatureFlagOverrideCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeatureFlagService {
    private static final Pattern REVISION_HEADER = Pattern.compile("^(?:W/)?\\\"?(\\d+)\\\"?$");
    private static final List<String> MUTABLE_SCOPE_TYPES = List.of("TENANT", "PLANT", "LINE");
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(
                    "bpi.ui", "BPI 导航入口", "控制旧平台是否展示 BPI 导航入口。",
                    "MEDIUM", "PENDING_SHELL_INTEGRATION", false,
                    "旧平台导航尚未读取该开关，禁止制造已生效的假象。"),
            new Definition(
                    "bpi.commands", "批次人工命令", "控制候选确认、驳回和批次状态命令。",
                    "HIGH", "ENFORCED", true, null),
            new Definition(
                    "bpi.rule-management", "规则与拓扑管理", "控制拓扑、规则、回放和发布类写操作。",
                    "HIGH", "ENFORCED", true, null),
            new Definition(
                    "bpi.shadow-only", "影子模式", "保证 Phase 1 只生成影子事实，不写生产业务状态。",
                    "CRITICAL", "CODE_INVARIANT", false,
                    "Phase 1 必须保持 shadow-only，不能通过运行页面关闭。"),
            new Definition(
                    "bpi.auto-confirm", "候选自动确认", "允许高置信候选绕过人工确认。",
                    "CRITICAL", "PHASE_LOCKED", false,
                    "未完成真实 7-14 天影子验收，Phase 2 自动确认门禁未开放。"),
            new Definition(
                    "bpi.wms-link", "WMS 完工入库联动", "允许 BPI 发起幂等 WMS 完工入库命令。",
                    "CRITICAL", "PHASE_LOCKED", false,
                    "QCS/WMS Phase 2 契约和真实写回验收尚未完成。"));

    private final FeatureFlagPostgresRepository repository;
    private final BpiPostgresRepository sharedRepository;
    private final CanonicalJson canonicalJson;
    private final ObjectMapper objectMapper;

    public FeatureFlagService(
            FeatureFlagPostgresRepository repository,
            BpiPostgresRepository sharedRepository,
            CanonicalJson canonicalJson,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.sharedRepository = sharedRepository;
        this.canonicalJson = canonicalJson;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagView> list(
            ActorContext actor,
            String plantId,
            String lineId,
            String selectedScopeType) {
        String scopeType = normalizeScopeType(selectedScopeType);
        assertConcreteScope(actor, plantId, lineId);
        return views(actor, plantId, lineId, scopeType);
    }

    @Transactional(timeout = 15)
    public CommandResult<FeatureFlagView> change(
            ActorContext actor,
            String flagKey,
            String idempotencyKey,
            String ifMatch,
            FeatureFlagOverrideCommand command,
            String traceId) {
        validateHeaders(idempotencyKey, ifMatch);
        long expectedRevision = parseRevision(ifMatch);
        String scopeType = normalizeScopeType(command.scopeType());
        String mode = normalizeMode(command.mode());
        assertConcreteScope(actor, command.plantId(), command.lineId());
        assertMutationScope(actor, scopeType, command.plantId(), command.lineId());
        Definition definition = definition(flagKey);
        if (!definition.editable()) {
            throw new BpiValidationException(definition.blockedReason());
        }
        if ("SET".equals(mode) && command.enabled() == null) {
            throw new BpiValidationException("enabled is required when mode is SET.");
        }

        String scopeKey = scopeKey(actor, scopeType, command.plantId(), command.lineId());
        String path = "/bpi/v1/feature-flags/" + flagKey;
        String checksum = Checksums.sha256(
                flagKey + "|" + expectedRevision + "|" + canonicalJson.write(command));
        CommandResult<FeatureFlagView> replay = replay(
                actor, idempotencyKey, path, checksum, new TypeReference<FeatureFlagView>() {});
        if (replay != null) return replay;

        FeatureFlagRecord before = repository.lockOverride(
                actor.tenantId(), scopeType, scopeKey, flagKey);
        assertRevision(before, expectedRevision);
        FeatureFlagRecord after;
        String action;
        if ("INHERIT".equals(mode)) {
            if (before == null || !before.active()) {
                throw new BpiConflictException(
                        "Feature flag already inherits from its parent scope.", before == null ? 0 : before.revision());
            }
            after = repository.updateOverride(
                    actor, before, expectedRevision, before.enabled(), false, command.reason());
            action = "FEATURE_FLAG_OVERRIDE_REMOVED";
        } else {
            boolean enabled = Boolean.TRUE.equals(command.enabled());
            if (before != null && before.active() && before.enabled() == enabled) {
                throw new BpiValidationException("Feature flag override already has the requested value.");
            }
            if (before == null) {
                after = repository.insertOverride(
                        actor, UUID.randomUUID(), scopeType, scopeKey, flagKey, enabled, command.reason());
            } else {
                after = repository.updateOverride(
                        actor, before, expectedRevision, enabled, true, command.reason());
            }
            action = enabled ? "FEATURE_FLAG_ENABLED" : "FEATURE_FLAG_DISABLED";
        }

        repository.insertAudit(actor, command.plantId(), command.lineId(), before, after,
                action, command.reason(), traceId);
        FeatureFlagView result = views(
                actor, command.plantId(), command.lineId(), scopeType).stream()
                .filter(item -> item.flagKey().equals(flagKey))
                .findFirst()
                .orElseThrow();
        complete(actor, idempotencyKey, result);
        return new CommandResult<>(result, false);
    }

    private List<FeatureFlagView> views(
            ActorContext actor,
            String plantId,
            String lineId,
            String selectedScopeType) {
        List<String> keys = DEFINITIONS.stream().map(Definition::flagKey).toList();
        List<FeatureFlagRecord> records = repository.listRelevant(actor.tenantId(), keys);
        String selectedScopeKey = scopeKey(actor, selectedScopeType, plantId, lineId);
        return DEFINITIONS.stream().map(definition -> {
            FeatureFlagRecord effective = records.stream()
                    .filter(FeatureFlagRecord::active)
                    .filter(item -> item.flagKey().equals(definition.flagKey()))
                    .filter(item -> applies(item, actor.tenantId(), plantId, lineId))
                    .max(Comparator.comparingInt(item -> priority(item, actor.tenantId())))
                    .orElse(null);
            FeatureFlagRecord selected = records.stream()
                    .filter(item -> item.tenantId().equals(actor.tenantId()))
                    .filter(item -> item.flagKey().equals(definition.flagKey()))
                    .filter(item -> item.scopeType().equals(selectedScopeType))
                    .filter(item -> item.scopeKey().equals(selectedScopeKey))
                    .findFirst()
                    .orElse(null);
            return new FeatureFlagView(
                    definition.flagKey(), definition.displayName(), definition.description(),
                    definition.riskLevel(), effective != null && effective.enabled(),
                    effective == null ? "DEFAULT_DENY" : effective.scopeType(),
                    effective == null ? "-" : effective.scopeKey(),
                    effective == null ? null : effective.revision(),
                    selectedScopeType, selectedScopeKey, selected != null,
                    selected != null && selected.active(), selected == null ? null : selected.enabled(),
                    selected == null ? 0 : selected.revision(),
                    selected == null ? null : selected.updatedBy(),
                    selected == null ? null : selected.updatedAt(),
                    selected == null ? null : selected.lastReason(),
                    definition.enforcementStatus(), definition.editable(), definition.blockedReason());
        }).toList();
    }

    private boolean applies(
            FeatureFlagRecord record,
            String tenantId,
            String plantId,
            String lineId) {
        if (record.tenantId().equals("*") && record.scopeType().equals("GLOBAL")
                && record.scopeKey().equals("*")) return true;
        if (!record.tenantId().equals(tenantId)) return false;
        return switch (record.scopeType()) {
            case "GLOBAL" -> record.scopeKey().equals("*");
            case "TENANT" -> record.scopeKey().equals(tenantId);
            case "PLANT" -> record.scopeKey().equals(plantId);
            case "LINE" -> record.scopeKey().equals(lineId);
            default -> false;
        };
    }

    private int priority(FeatureFlagRecord record, String tenantId) {
        if (!record.tenantId().equals(tenantId)) return 10;
        return switch (record.scopeType()) {
            case "LINE" -> 50;
            case "PLANT" -> 40;
            case "TENANT" -> 30;
            case "GLOBAL" -> 20;
            default -> 0;
        };
    }

    private void assertRevision(FeatureFlagRecord current, long expectedRevision) {
        if (current == null && expectedRevision != 0) {
            throw new BpiConflictException("A new feature flag override must use If-Match 0.", 0L);
        }
        if (current != null && current.revision() != expectedRevision) {
            throw new BpiConflictException("Feature flag override revision is stale.", current.revision());
        }
    }

    private void assertConcreteScope(ActorContext actor, String plantId, String lineId) {
        if (plantId == null || plantId.isBlank() || lineId == null || lineId.isBlank()) {
            throw new BpiValidationException("plantId and lineId are required for feature flag resolution.");
        }
        if (!actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Token scope does not allow this feature flag target.");
        }
    }

    private void assertMutationScope(
            ActorContext actor,
            String scopeType,
            String plantId,
            String lineId) {
        if ("TENANT".equals(scopeType)
                && (!actor.plantIds().contains("*") || !actor.lineIds().contains("*"))) {
            throw new BpiForbiddenException("Tenant overrides require unrestricted tenant scope.");
        }
        if ("PLANT".equals(scopeType)
                && (!(actor.plantIds().contains("*") || actor.plantIds().contains(plantId))
                    || !actor.lineIds().contains("*"))) {
            throw new BpiForbiddenException("Plant overrides require access to every line in the plant.");
        }
        if ("LINE".equals(scopeType) && !actor.canAccess(plantId, lineId)) {
            throw new BpiForbiddenException("Line override is outside the token scope.");
        }
    }

    private String scopeKey(
            ActorContext actor,
            String scopeType,
            String plantId,
            String lineId) {
        return switch (scopeType) {
            case "TENANT" -> actor.tenantId();
            case "PLANT" -> plantId;
            case "LINE" -> lineId;
            default -> throw new BpiValidationException("Unsupported feature flag scope type.");
        };
    }

    private String normalizeScopeType(String value) {
        String normalized = value == null ? "LINE" : value.trim().toUpperCase();
        if (!MUTABLE_SCOPE_TYPES.contains(normalized)) {
            throw new BpiValidationException("scopeType must be TENANT, PLANT or LINE.");
        }
        return normalized;
    }

    private String normalizeMode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!List.of("SET", "INHERIT").contains(normalized)) {
            throw new BpiValidationException("mode must be SET or INHERIT.");
        }
        return normalized;
    }

    private Definition definition(String flagKey) {
        return DEFINITIONS.stream().filter(item -> item.flagKey().equals(flagKey))
                .findFirst()
                .orElseThrow(() -> new BpiValidationException("Unsupported BPI feature flag."));
    }

    private <T> CommandResult<T> replay(
            ActorContext actor,
            String idempotencyKey,
            String path,
            String checksum,
            TypeReference<T> type) {
        boolean owner = sharedRepository.reserveIdempotency(
                UUID.randomUUID(), actor.tenantId(), idempotencyKey, "POST", path, checksum);
        if (owner) return null;
        IdempotencyRecord previous = sharedRepository.lockIdempotency(actor.tenantId(), idempotencyKey);
        if (!"POST".equals(previous.method()) || !path.equals(previous.resourcePath())
                || !checksum.equals(previous.requestChecksum())) {
            throw new BpiConflictException("Idempotency-Key was reused with a different request.", null);
        }
        if ("COMPLETED".equals(previous.state()) && previous.responseBody() != null) {
            return new CommandResult<>(sharedRepository.readJson(previous.responseBody(), type), true);
        }
        throw new BpiConflictException("The command is still processing.", null);
    }

    private void complete(ActorContext actor, String idempotencyKey, Object response) {
        sharedRepository.completeIdempotency(
                actor.tenantId(), idempotencyKey, 200, writeJson(response));
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize feature flag command", exception);
        }
    }

    private record Definition(
            String flagKey,
            String displayName,
            String description,
            String riskLevel,
            String enforcementStatus,
            boolean editable,
            String blockedReason) {
    }
}
