package com.mapletct.ftmes.bpi.application;

import java.util.UUID;

public record SourceSequenceEvidenceIngestResult(
        UUID evidenceId,
        long revision,
        boolean replayed,
        boolean stale,
        boolean transitioned) {
}
