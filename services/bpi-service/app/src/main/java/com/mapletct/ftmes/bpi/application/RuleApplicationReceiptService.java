package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.domain.RuleApplicationTarget;
import com.mapletct.ftmes.bpi.domain.RuleVersionView;
import com.mapletct.ftmes.bpi.infrastructure.application.RuleApplicationPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.RulePostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class RuleApplicationReceiptService {
    private static final UUID INBOX_NAMESPACE = UUID.fromString("ca8aaf29-5fa8-5192-8b2d-12358b81226d");
    private static final String SOURCE = "bpi.boundary.rule-application.v1";

    private final BpiPostgresRepository inboxRepository;
    private final RulePostgresRepository ruleRepository;
    private final RuleApplicationPostgresRepository applicationRepository;

    public RuleApplicationReceiptService(
            BpiPostgresRepository inboxRepository,
            RulePostgresRepository ruleRepository,
            RuleApplicationPostgresRepository applicationRepository) {
        this.inboxRepository = inboxRepository;
        this.ruleRepository = ruleRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(timeout = 15)
    public RuleVersionView apply(BoundaryRuleApplicationV1 event, String payloadChecksum) {
        UUID publicationId = parsePublicationId(event.getPublicationEventId());
        RuleApplicationTarget target = applicationRepository.lockTarget(event.getTenantId(), publicationId);
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
        if (!firstDelivery || terminalReplay(target.applicationStatus(), event.getStatus())) {
            return ruleRepository.findRule(actor, target.ruleId());
        }

        long afterRevision = applicationRepository.updateApplication(target, event);
        applicationRepository.insertAudit(target, event, afterRevision, traceId(event));
        return ruleRepository.findRule(actor, target.ruleId());
    }

    private void validateIdentity(
            RuleApplicationTarget target,
            BoundaryRuleApplicationV1 event) {
        if (!target.plantId().equals(event.getPlantId())
                || !target.lineId().equals(event.getLineId())
                || !target.ruleCode().equals(event.getRuleCode())
                || !target.ruleVersion().equals(event.getRuleVersion())
                || !target.ruleChecksum().equals(event.getChecksum())) {
            throw new BpiValidationException(
                    "Flink rule application identity or checksum does not match the publication.");
        }
    }

    private static boolean terminalReplay(
            String current,
            BoundaryRuleApplicationStatusV1 incoming) {
        if ("APPLIED".equals(current)) {
            return true;
        }
        return "REJECTED".equals(current) && incoming == BoundaryRuleApplicationStatusV1.REJECTED;
    }

    private static UUID parsePublicationId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            throw new BpiValidationException("publication_event_id must be a UUID.");
        }
    }

    private static ActorContext actor(BoundaryRuleApplicationV1 event) {
        return new ActorContext(
                event.getTenantId(),
                "flink:" + event.getDeploymentId(),
                Set.of("BPI_EVENT_INGEST"),
                Set.of(event.getPlantId()),
                Set.of(event.getLineId()));
    }

    private static String traceId(BoundaryRuleApplicationV1 event) {
        String traceId = event.getHeadersOrDefault("trace_id", "");
        String result = traceId.isBlank() ? event.getEventId() : traceId;
        return result.length() <= 128 ? result : result.substring(0, 128);
    }
}
