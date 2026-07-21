package com.mapletct.ftmes.bpi.infrastructure.dataset;

import com.mapletct.ftmes.bpi.application.DatasetManifestProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "bpi.dataset-manifest",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DatasetManifestDispatcher {
    private final DatasetManifestProcessor processor;
    private final DatasetManifestProperties properties;

    public DatasetManifestDispatcher(
            DatasetManifestProcessor processor,
            DatasetManifestProperties properties) {
        this.processor = processor;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${bpi.dataset-manifest.poll-delay:2s}")
    public void dispatchPending() {
        for (int index = 0; index < properties.batchSize(); index++) {
            if (!processor.processOne()) return;
        }
    }
}
