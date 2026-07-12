package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;

import java.util.List;
import java.util.Objects;

public record ProductionContextJoinState(
        List<ProductionContextEventV1> contexts,
        List<PendingContextPoint> pending) {

    public ProductionContextJoinState {
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(pending, "pending");
        contexts = List.copyOf(contexts);
        pending = List.copyOf(pending);
    }

    public static ProductionContextJoinState empty() {
        return new ProductionContextJoinState(List.of(), List.of());
    }
}
