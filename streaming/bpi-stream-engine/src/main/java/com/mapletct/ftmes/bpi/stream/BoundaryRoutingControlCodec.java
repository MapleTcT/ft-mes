package com.mapletct.ftmes.bpi.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

final class BoundaryRoutingControlCodec {

    private static final int MAGIC = 0x42524354;
    private static final int VERSION = 1;
    private static final int MAX_PAYLOAD_BYTES = 8 * 1024 * 1024;

    private BoundaryRoutingControlCodec() {
    }

    static byte[] rule(byte[] payload) {
        return encode(Kind.RULE, payload);
    }

    static byte[] pointCatalog(byte[] payload) {
        return encode(Kind.POINT_CATALOG, payload);
    }

    static Decoded decode(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalStateException("routing control payload is required");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported routing control header");
            }
            Kind kind = Kind.fromCode(input.readUnsignedByte());
            int length = input.readInt();
            if (length <= 0 || length > MAX_PAYLOAD_BYTES) {
                throw new IOException("invalid routing control payload length: " + length);
            }
            byte[] payload = input.readNBytes(length);
            if (payload.length != length || input.read() != -1) {
                throw new IOException("truncated or trailing routing control data");
            }
            return new Decoded(kind, payload);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode routing control", error);
        }
    }

    private static byte[] encode(Kind kind, byte[] payload) {
        if (payload == null || payload.length == 0 || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("routing control payload must be 1-8388608 bytes");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(payload.length + 13);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeByte(kind.code);
                output.writeInt(payload.length);
                output.write(payload);
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode routing control", error);
        }
    }

    enum Kind {
        RULE(1),
        POINT_CATALOG(2);

        private final int code;

        Kind(int code) {
            this.code = code;
        }

        private static Kind fromCode(int code) {
            return Arrays.stream(values())
                    .filter(value -> value.code == code)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unsupported routing control kind: " + code));
        }
    }

    record Decoded(Kind kind, byte[] payload) {
        Decoded {
            payload = Arrays.copyOf(payload, payload.length);
        }
    }
}
