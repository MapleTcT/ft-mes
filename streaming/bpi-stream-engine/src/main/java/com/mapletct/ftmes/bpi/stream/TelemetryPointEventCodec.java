package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class TelemetryPointEventCodec {

    private static final int MAGIC = 0x42505445;
    private static final int VERSION = 1;
    private static final int MAX_MESSAGE_BYTES = 1_048_576;

    private TelemetryPointEventCodec() {
    }

    public static byte[] encode(TelemetryPointEvent event) {
        byte[] envelope = event.envelope().toByteArray();
        if (envelope.length > MAX_MESSAGE_BYTES) {
            throw new IllegalStateException("telemetry envelope exceeds one MiB");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeInt(envelope.length);
                output.write(envelope);
                output.writeInt(event.pointIndex());
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode telemetry point event", error);
        }
    }

    public static TelemetryPointEvent decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported telemetry point event header");
            }
            int length = input.readInt();
            if (length < 0 || length > MAX_MESSAGE_BYTES) {
                throw new IOException("invalid telemetry envelope length: " + length);
            }
            byte[] envelopeBytes = input.readNBytes(length);
            if (envelopeBytes.length != length) {
                throw new IOException("truncated telemetry envelope");
            }
            TelemetryPointEvent event = new TelemetryPointEvent(
                    TelemetryEnvelopeV1.parseFrom(envelopeBytes), input.readInt());
            if (input.read() != -1) {
                throw new IOException("telemetry point event has trailing data");
            }
            return event;
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode telemetry point event", error);
        }
    }
}
