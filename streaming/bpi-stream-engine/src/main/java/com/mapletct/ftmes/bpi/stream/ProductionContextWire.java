package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;

public final class ProductionContextWire {

    private ProductionContextWire() {
    }

    public static ProductionContextEventV1 decode(byte[] bytes) {
        try {
            return ProductionContextEventV1.parseFrom(bytes);
        } catch (InvalidProtocolBufferException error) {
            throw new IllegalStateException("cannot decode production context event", error);
        }
    }

    public static String scopeKey(byte[] bytes) {
        return TelemetryPointEvent.contextScopeKey(decode(bytes));
    }
}
