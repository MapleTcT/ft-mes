package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProductionContextJoinStateCodec {

    private static final int MAGIC = 0x42504A53;
    private static final int VERSION = 1;
    private static final int MAX_ITEMS = 10_000;
    private static final int MAX_MESSAGE_BYTES = 1_048_576;

    private ProductionContextJoinStateCodec() {
    }

    public static byte[] encode(ProductionContextJoinState state) {
        if (state.contexts().size() > MAX_ITEMS || state.pending().size() > MAX_ITEMS) {
            throw new IllegalStateException("context join state exceeds the per-key item limit");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                List<ProductionContextEventV1> contexts = new ArrayList<>(state.contexts());
                contexts.sort(Comparator.comparingLong(ProductionContextEventV1::getEffectiveFromMs)
                        .thenComparingLong(ProductionContextEventV1::getContextRevision)
                        .thenComparing(ProductionContextEventV1::getEventId));
                output.writeInt(contexts.size());
                for (ProductionContextEventV1 context : contexts) {
                    writeBytes(output, context.toByteArray());
                }
                List<PendingContextPoint> pending = new ArrayList<>(state.pending());
                pending.sort(Comparator.comparingLong(PendingContextPoint::deadlineEpochMs)
                        .thenComparing(item -> item.telemetry().identity()));
                output.writeInt(pending.size());
                for (PendingContextPoint item : pending) {
                    writeBytes(output, item.telemetry().envelope().toByteArray());
                    output.writeInt(item.telemetry().pointIndex());
                    output.writeLong(item.deadlineEpochMs());
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode production context join state", error);
        }
    }

    public static ProductionContextJoinState decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported production context join state header");
            }
            int contextCount = bounded(input.readInt());
            List<ProductionContextEventV1> contexts = new ArrayList<>(contextCount);
            for (int index = 0; index < contextCount; index++) {
                contexts.add(ProductionContextEventV1.parseFrom(readBytes(input)));
            }
            int pendingCount = bounded(input.readInt());
            List<PendingContextPoint> pending = new ArrayList<>(pendingCount);
            for (int index = 0; index < pendingCount; index++) {
                TelemetryEnvelopeV1 envelope = TelemetryEnvelopeV1.parseFrom(readBytes(input));
                pending.add(new PendingContextPoint(
                        new TelemetryPointEvent(envelope, input.readInt()), input.readLong()));
            }
            if (input.read() != -1) {
                throw new IOException("production context join state has trailing data");
            }
            return new ProductionContextJoinState(contexts, pending);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode production context join state", error);
        }
    }

    private static int bounded(int count) throws IOException {
        if (count < 0 || count > MAX_ITEMS) {
            throw new IOException("invalid production context join item count: " + count);
        }
        return count;
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        if (value.length > MAX_MESSAGE_BYTES) {
            throw new IOException("context join message exceeds one MiB");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("invalid context join message length: " + length);
        }
        byte[] value = input.readNBytes(length);
        if (value.length != length) {
            throw new IOException("truncated context join message");
        }
        return value;
    }
}
