package com.mapletct.ftmes.bpi.domain;

public enum BatchState {
    ACTIVE,
    SUSPENDED,
    CLOSED_RAW,
    RECONCILING,
    AMENDING,
    REVIEW_REQUIRED,
    WAIT_QA,
    REJECTED,
    DISPOSED,
    REWORK,
    RELEASED,
    INBOUNDED
}
