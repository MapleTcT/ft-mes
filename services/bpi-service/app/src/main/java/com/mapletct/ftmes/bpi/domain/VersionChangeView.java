package com.mapletct.ftmes.bpi.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record VersionChangeView(
        String path,
        String changeType,
        JsonNode beforeValue,
        JsonNode afterValue) {
}
