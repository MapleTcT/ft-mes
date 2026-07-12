package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ContextualTelemetryPointCodec {

    private static final int MAGIC = 0x42504354;
    private static final int VERSION = 1;
    private static final int MAX_MESSAGE_BYTES = 1_048_576;

    private ContextualTelemetryPointCodec() {
    }

    public static byte[] encode(ContextualTelemetryPoint value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                writeBytes(output, value.telemetry().envelope().toByteArray());
                output.writeInt(value.telemetry().pointIndex());
                writeBytes(output, value.context().toByteArray());
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode contextual telemetry point", error);
        }
    }

    public static ContextualTelemetryPoint decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported contextual telemetry point header");
            }
            TelemetryEnvelopeV1 envelope = TelemetryEnvelopeV1.parseFrom(readBytes(input));
            TelemetryPointEvent telemetry = new TelemetryPointEvent(envelope, input.readInt());
            ProductionContextEventV1 context = ProductionContextEventV1.parseFrom(readBytes(input));
            if (input.read() != -1) {
                throw new IOException("contextual telemetry point has trailing data");
            }
            return new ContextualTelemetryPoint(telemetry, context);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode contextual telemetry point", error);
        }
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        if (value.length > MAX_MESSAGE_BYTES) {
            throw new IOException("contextual telemetry message exceeds one MiB");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("invalid contextual telemetry message length: " + length);
        }
        byte[] value = input.readNBytes(length);
        if (value.length != length) {
            throw new IOException("truncated contextual telemetry message");
        }
        return value;
    }
}
