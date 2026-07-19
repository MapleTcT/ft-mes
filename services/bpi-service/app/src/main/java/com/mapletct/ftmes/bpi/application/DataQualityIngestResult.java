package com.mapletct.ftmes.bpi.application;

import java.util.UUID;

public record DataQualityIngestResult(
        UUID incidentId,
        boolean duplicate,
        boolean created,
        boolean reopened) {
}
