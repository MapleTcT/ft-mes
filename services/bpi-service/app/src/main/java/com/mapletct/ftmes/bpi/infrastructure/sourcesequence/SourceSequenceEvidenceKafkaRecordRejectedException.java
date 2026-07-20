package com.mapletct.ftmes.bpi.infrastructure.sourcesequence;

public class SourceSequenceEvidenceKafkaRecordRejectedException extends RuntimeException {
    public SourceSequenceEvidenceKafkaRecordRejectedException(String message) {
        super(message);
    }

    public SourceSequenceEvidenceKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
