package com.mapletct.ftmes.bpi.domain;

public record TopologyValidationIssue(
        String code,
        String path,
        String severity,
        String message) {
}
