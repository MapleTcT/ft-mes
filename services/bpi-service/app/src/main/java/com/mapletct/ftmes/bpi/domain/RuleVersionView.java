package com.mapletct.ftmes.bpi.domain;

import java.util.Map;
import java.util.UUID;

public record RuleVersionView(
        UUID id,
        String code,
        String version,
        String state,
        long revision,
        String plantId,
        String lineId,
        String topologyVersion,
        String checksum,
        Map<String, Object> ast,
        UUID latestSimulationId) {
}
