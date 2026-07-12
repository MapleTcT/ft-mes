package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.util.ArrayList;
import java.util.List;

public final class TelemetryEnvelopeFlattener {

    private TelemetryEnvelopeFlattener() {
    }

    public static List<TelemetryPointEvent> flatten(TelemetryEnvelopeV1 envelope) {
        List<TelemetryPointEvent> points = new ArrayList<>(envelope.getPointsCount());
        for (int index = 0; index < envelope.getPointsCount(); index++) {
            points.add(new TelemetryPointEvent(envelope, index));
        }
        return List.copyOf(points);
    }

    public static List<byte[]> flattenToBytes(TelemetryEnvelopeV1 envelope) {
        return flatten(envelope).stream().map(TelemetryPointEventCodec::encode).toList();
    }
}
