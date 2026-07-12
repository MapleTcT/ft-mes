package com.mapletct.ftmes.bpi.stream;

import java.util.List;
import java.util.Objects;

public record BoundaryRoutingResult(
        List<BoundaryStreamInput> inputs,
        List<BoundaryRoutingIssue> issues) {

    public BoundaryRoutingResult {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(issues, "issues");
        inputs = List.copyOf(inputs);
        issues = List.copyOf(issues);
    }
}
