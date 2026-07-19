package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.infrastructure.postgres.BpiPostgresRepository;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DataQualityPostgresRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DataQualityIngestionService {
    private static final UUID INCIDENT_NAMESPACE =
            UUID.fromString("95f8312e-c37a-5f37-a4f2-f92ba0bd1d27");
    private static final String INBOX_SOURCE = "BPI_DATA_QUALITY_KAFKA";

    private final BpiPostgresRepository sharedRepository;
    private final DataQualityPostgresRepository repository;

    public DataQualityIngestionService(
            BpiPostgresRepository sharedRepository,
            DataQualityPostgresRepository repository) {
        this.sharedRepository = sharedRepository;
        this.repository = repository;
    }

    @Transactional(timeout = 15)
    public DataQualityIngestResult ingest(DataQualityEventV1 event, String actorId) {
        String source = event.getHeadersOrDefault("stage", "");
        UUID incidentId = UuidV5.from(INCIDENT_NAMESPACE, String.join("|",
                event.getTenantId(), event.getPlantId(), event.getLineId(), source,
                event.getDeviceId(), event.getPropertyId(), event.getIssueCode()));
        boolean owner = sharedRepository.recordInbox(
                UUID.randomUUID(), event.getTenantId(), INBOX_SOURCE,
                event.getEventId(), event.getEventId(), Checksums.sha256(event.toByteArray()));
        if (!owner) return new DataQualityIngestResult(incidentId, true, false, false);
        DataQualityPostgresRepository.IngestionResult result =
                repository.ingest(incidentId, event, source, actorId);
        return new DataQualityIngestResult(
                result.incidentId(), false, result.created(), result.reopened());
    }
}
