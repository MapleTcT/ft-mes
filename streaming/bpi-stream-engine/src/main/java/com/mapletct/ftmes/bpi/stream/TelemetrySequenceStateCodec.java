package com.mapletct.ftmes.bpi.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class TelemetrySequenceStateCodec {

    private static final int MAGIC = 0x42505351;
    private static final int VERSION = 1;

    private TelemetrySequenceStateCodec() {
    }

    static byte[] encode(TelemetrySequenceState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeLong(state.sourceEpoch());
                output.writeLong(state.highestSequence());
                output.writeUTF(state.eventId());
                output.writeUTF(state.payloadSha256());
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode telemetry sequence state", error);
        }
    }

    static TelemetrySequenceState decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported telemetry sequence state header");
            }
            TelemetrySequenceState state = new TelemetrySequenceState(
                    input.readLong(), input.readLong(), input.readUTF(), input.readUTF());
            if (input.read() != -1) {
                throw new IOException("telemetry sequence state has trailing data");
            }
            return state;
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode telemetry sequence state", error);
        }
    }
}
