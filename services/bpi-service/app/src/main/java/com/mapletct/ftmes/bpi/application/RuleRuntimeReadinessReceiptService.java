package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import com.mapletct.ftmes.bpi.domain.RuleRuntimeReadinessTarget;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.infrastructure.application.RuleRuntimeReadinessPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.RulePostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class RuleRuntimeReadinessReceiptService {
    private static final UUID INBOX_NAMESPACE = UUID.fromString("f09374c1-0a4b-578c-a67f-7c57b73fcdf9");
    private static final String SOURCE = "bpi.boundary.rule-runtime-readiness.v1";

    private final BpiPostgresRepository inboxRepository;
    private final RulePostgresRepository ruleRepository;
    private final RuleRuntimeReadinessPostgresRepository readinessRepository;

    public RuleRuntimeReadinessReceiptService(
            BpiPostgresRepository inboxRepository,
            RulePostgresRepository ruleRepository,
            RuleRuntimeReadinessPostgresRepository readinessRepository) {
        this.inboxRepository = inboxRepository;
        this.ruleRepository = ruleRepository;
        this.readinessRepository = readinessRepository;
    }

    @Transactional(timeout = 15)
    public RuleVersionView apply(BoundaryRuleRuntimeReadinessV1 event, String payloadChecksum) {
        UUID publicationId = parsePublicationId(event.getPublicationEventId());
        RuleRuntimeReadinessTarget target = readinessRepository.lockTarget(event.getTenantId(), publicationId);
        validateIdentity(target, event);
        if (!"PUBLISHED".equals(target.publicationStatus())) {
            throw new BpiConflictException(
                    "Rule publication has not reached Kafka PUBLISHED state yet.",
                    target.publicationRevision());
        }

        UUID inboxId = UuidV5.from(
                INBOX_NAMESPACE, event.getTenantId() + "|" + SOURCE + "|" + event.getEventId());
        boolean firstDelivery = inboxRepository.recordInbox(
                inboxId,
                event.getTenantId(),
                SOURCE,
                event.getEventId(),
                event.getEventId(),
                payloadChecksum);
        ActorContext actor = actor(event);
        if (!firstDelivery || event.getEventId().equals(target.runtimeReadinessEventId())) {
            return ruleRepository.findRule(actor, target.ruleId());
        }

        Instant incomingObservedAt = Instant.ofEpochMilli(event.getObservedAtMs());
        if (target.runtimeReadinessObservedAt() != null) {
            int order = incomingObservedAt.compareTo(target.runtimeReadinessObservedAt());
            if (order < 0) {
                return ruleRepository.findRule(actor, target.ruleId());
            }
            if (order == 0) {
                throw new BpiConflictException(
                        "Different runtime-readiness events share the same observed_at timestamp.",
                        target.publicationRevision());
            }
        }

        long afterRevision = readinessRepository.updateReadiness(target, event);
        readinessRepository.insertAudit(target, event, afterRevision, traceId(event));
        return ruleRepository.findRule(actor, target.ruleId());
    }

    private static void validateIdentity(
            RuleRuntimeReadinessTarget target,
            BoundaryRuleRuntimeReadinessV1 event) {
        if (!target.plantId().equals(event.getPlantId())
                || !target.lineId().equals(event.getLineId())
                || !target.ruleCode().equals(event.getRuleCode())
                || !target.ruleVersion().equals(event.getRuleVersion())
                || !target.ruleChecksum().equals(event.getChecksum())) {
            throw new BpiValidationException(
                    "Flink runtime-readiness identity or checksum does not match the publication.");
        }
    }

    private static UUID parsePublicationId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            throw new BpiValidationException("publication_event_id must be a UUID.");
        }
    }

    private static ActorContext actor(BoundaryRuleRuntimeReadinessV1 event) {
        return new ActorContext(
                event.getTenantId(),
                "flink:" + event.getDeploymentId(),
                Set.of("BPI_EVENT_INGEST"),
                Set.of(event.getPlantId()),
                Set.of(event.getLineId()));
    }

    private static String traceId(BoundaryRuleRuntimeReadinessV1 event) {
        String traceId = event.getHeadersOrDefault("trace_id", "");
        String result = traceId.isBlank() ? event.getEventId() : traceId;
        return result.length() <= 128 ? result : result.substring(0, 128);
    }
}
