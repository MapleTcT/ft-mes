package com.mapletct.ftmes.bpi.infrastructure.postgres;

import com.mapletct.ftmes.bpi.domain.BatchCandidate;

import java.util.UUID;

public record PersistedCandidate(
        BatchCandidate candidate,
        UUID topologyVersionId,
        UUID ruleVersionId) {
}
