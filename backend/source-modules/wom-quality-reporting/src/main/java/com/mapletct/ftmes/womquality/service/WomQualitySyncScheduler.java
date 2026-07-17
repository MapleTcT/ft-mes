package com.mapletct.ftmes.womquality.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WomQualitySyncScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WomQualitySyncScheduler.class);

    private final WomQualityService service;

    public WomQualitySyncScheduler(WomQualityService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${wom.quality.sync-delay-ms:30000}")
    public void synchronizePendingReports() {
        List<Map<String, Object>> candidates = service.syncCandidates(20);
        for (Map<String, Object> candidate : candidates) {
            try {
                service.synchronize(
                    String.valueOf(candidate.get("tenant_id")),
                    String.valueOf(candidate.get("id")),
                    "system-retry");
            } catch (RuntimeException exception) {
                LOGGER.warn("Cannot synchronize WOM bad quantity report {}", candidate.get("id"), exception);
            }
        }
    }
}
