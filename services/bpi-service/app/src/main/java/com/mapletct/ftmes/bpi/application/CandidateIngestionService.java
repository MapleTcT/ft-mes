package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import com.mapletct.ftmes.bpi.domain.BatchCandidate;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.interfaces.rest.CandidateIngestRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CandidateIngestionService {
    private static final UUID CANDIDATE_NAMESPACE = UUID.fromString("6f15e765-2f3b-5d1c-93f2-77d5a7ed6622");
    private static final UUID INBOX_NAMESPACE = UUID.fromString("8a92f23e-d20f-514e-841b-e4a90ca6460e");
    private static final String SOURCE = "bpi.batch.candidate.v1";

    private final BpiPostgresRepository repository;
    private final ObjectMapper objectMapper;

    public CandidateIngestionService(BpiPostgresRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(timeout = 15)
    public BatchCandidate ingest(ActorContext actor, CandidateIngestRequest request) {
        if (!actor.canAccess(request.plantId(), request.lineId())) {
            throw new BpiForbiddenException("Token scope does not allow this plant/line.");
        }
        VersionRefs versions = repository.resolveVersions(
                actor.tenantId(), request.topologyCode(), request.topologyVersion(),
                request.ruleCode(), request.ruleVersion());
        String checksum = Checksums.sha256(writePayload(request));
        UUID inboxId = UuidV5.from(INBOX_NAMESPACE, actor.tenantId() + "|" + SOURCE + "|" + request.candidateKey());
        repository.recordInbox(
                inboxId, actor.tenantId(), SOURCE, request.candidateKey().toString(), request.eventId(), checksum);

        UUID candidateId = UuidV5.from(
                CANDIDATE_NAMESPACE, actor.tenantId() + "|" + request.candidateKey());
        repository.insertCandidate(
                candidateId, request.candidateKey(), actor, request.plantId(), request.lineId(),
                request.boundaryType(), request.orderId(), request.boundaryTime(), request.confidence(),
                versions, request.evidence(), request.normalizedMissingSignals());
        return repository.findCandidateByKey(actor, request.candidateKey());
    }

    private String writePayload(CandidateIngestRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Candidate payload cannot be serialized", exception);
        }
    }
}
