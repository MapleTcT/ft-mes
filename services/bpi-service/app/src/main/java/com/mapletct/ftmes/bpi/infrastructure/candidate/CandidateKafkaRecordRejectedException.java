package com.mapletct.ftmes.bpi.infrastructure.candidate;

public class CandidateKafkaRecordRejectedException extends RuntimeException {

    public CandidateKafkaRecordRejectedException(String message) {
        super(message);
    }

    public CandidateKafkaRecordRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
