package com.mapletct.ftmes.bpi.domain;

public enum BatchState {
    ACTIVE,
    SUSPENDED,
    RECONCILING,
    WAIT_QA,
    RELEASED,
    INBOUNDED,
    CLOSED
}
