package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiConflictException;
import com.mapletct.ftmes.bpi.contract.v1.SourceSequenceEvidenceV1;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.sourcesequence.SourceSequenceEvidencePostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class SourceSequenceEvidenceIngestionService {
    private static final UUID EVIDENCE_NAMESPACE =
            UUID.fromString("2818b9fc-34c2-5af5-b2ed-e8c08e0582b7");
    private static final UUID INBOX_NAMESPACE =
            UUID.fromString("747326f8-a4c3-596c-ae7b-cee81304df2b");
    private static final String INBOX_SOURCE = "iot.source-sequence.evidence.v1";

    private final BpiPostgresRepository inboxRepository;
    private final SourceSequenceEvidencePostgresRepository repository;

    public SourceSequenceEvidenceIngestionService(
            BpiPostgresRepository inboxRepository,
            SourceSequenceEvidencePostgresRepository repository) {
        this.inboxRepository = inboxRepository;
        this.repository = repository;
    }

    @Transactional(timeout = 15)
    public SourceSequenceEvidenceIngestResult ingest(
            SourceSequenceEvidenceV1 event,
            String payloadChecksum,
            String actorId) {
        String identity = identity(event);
        UUID evidenceId = UuidV5.from(EVIDENCE_NAMESPACE, identity);
        UUID inboxId = UuidV5.from(
                INBOX_NAMESPACE,
                event.getTenantId() + "|" + INBOX_SOURCE + "|" + event.getEventId()
        );
        boolean firstDelivery = inboxRepository.recordInbox(
                inboxId,
                event.getTenantId(),
                INBOX_SOURCE,
                event.getEventId(),
                event.getEventId(),
                payloadChecksum
        );
        SourceSequenceEvidencePostgresRepository.CurrentEvidence current = repository.lockCurrent(event);
        if (!firstDelivery) {
            return new SourceSequenceEvidenceIngestResult(
                    current == null ? evidenceId : current.id(),
                    current == null ? 0L : current.revision(),
                    true,
                    false,
                    false
            );
        }

        Instant observedAt = Instant.ofEpochMilli(event.getObservedAtMs());
        if (current != null && !observedAt.isAfter(current.observedAt())) {
            if (observedAt.equals(current.observedAt())
                    && !event.getEventId().equals(current.sourceEventId())) {
                throw new BpiConflictException(
                        "Source sequence evidence reused observedAt with different content.",
                        current.revision()
                );
            }
            return new SourceSequenceEvidenceIngestResult(
                    current.id(), current.revision(), false, true, false);
        }

        boolean transitioned = current == null
                || !status(event).equals(current.status())
                || !Objects.equals(origin(event), current.sequenceOrigin())
                || !Objects.equals(epoch(event), current.sourceEpoch());
        long revision = repository.upsert(evidenceId, event, payloadChecksum, current);
        if (transitioned) {
            repository.insertAudit(
                    evidenceId,
                    event,
                    actorId,
                    current == null ? null : current.revision(),
                    revision
            );
        }
        return new SourceSequenceEvidenceIngestResult(
                evidenceId, revision, false, false, transitioned);
    }

    private static String identity(SourceSequenceEvidenceV1 event) {
        return String.join("|",
                event.getTenantId(),
                event.getSource(),
                event.getSourceInstance(),
                event.getPlantId(),
                event.getLineId(),
                event.getProductId(),
                event.getDeviceId(),
                event.getBindingFingerprint());
    }

    public static String status(SourceSequenceEvidenceV1 event) {
        return switch (event.getStatus()) {
            case SOURCE_SEQUENCE_EVIDENCE_DISABLED -> "DISABLED";
            case SOURCE_SEQUENCE_EVIDENCE_MISSING -> "MISSING";
            case SOURCE_SEQUENCE_EVIDENCE_PENDING -> "PENDING";
            case SOURCE_SEQUENCE_EVIDENCE_QUALIFIED -> "QUALIFIED";
            case SOURCE_SEQUENCE_EVIDENCE_EXPIRED -> "EXPIRED";
            default -> throw new IllegalArgumentException("Unsupported source sequence evidence status.");
        };
    }

    public static String origin(SourceSequenceEvidenceV1 event) {
        return switch (event.getSequenceOrigin()) {
            case DEVICE -> "DEVICE";
            case GATEWAY -> "GATEWAY";
            default -> null;
        };
    }

    public static Long epoch(SourceSequenceEvidenceV1 event) {
        return origin(event) == null ? null : event.getSourceEpoch();
    }
}
