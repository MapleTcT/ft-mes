package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiValidationException;
import com.mapletct.ftmes.bpi.domain.DatasetManifestBuild;
import com.mapletct.ftmes.bpi.domain.DatasetManifestClaim;
import com.mapletct.ftmes.bpi.domain.DatasetSampleSource;
import com.mapletct.ftmes.bpi.infrastructure.dataset.DatasetManifestProperties;
import com.mapletct.ftmes.bpi.infrastructure.postgres.DatasetPostgresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasetManifestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetManifestProcessor.class);

    private final DatasetPostgresRepository repository;
    private final DatasetManifestBuilder builder;
    private final DatasetManifestProperties properties;

    public DatasetManifestProcessor(
            DatasetPostgresRepository repository,
            DatasetManifestBuilder builder,
            DatasetManifestProperties properties) {
        this.repository = repository;
        this.builder = builder;
        this.properties = properties;
    }

    public boolean processOne() {
        DatasetManifestClaim claim = repository.claimPending(
                properties.claimTimeout(), properties.maxAttempts());
        if (claim == null) return false;
        try {
            List<DatasetSampleSource> sourceRows = repository.findSampleSources(
                    claim, DatasetManifestBuilder.MAX_SAMPLES + 1);
            DatasetManifestBuild build = builder.build(claim, sourceRows);
            repository.completeManifest(claim, build);
            return true;
        } catch (Exception exception) {
            String code = exception instanceof BpiValidationException
                    ? "DATASET_VALIDATION_FAILED" : "DATASET_MANIFEST_BUILD_FAILED";
            String detail = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            repository.failManifest(claim, code, detail);
            LOGGER.warn("BPI dataset manifest build failed for {}: {}", claim.snapshotId(), detail);
            return true;
        }
    }
}
