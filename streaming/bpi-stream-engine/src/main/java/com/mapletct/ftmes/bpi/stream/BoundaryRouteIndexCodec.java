package com.mapletct.ftmes.bpi.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

final class BoundaryRouteIndexCodec {

    private static final int MAGIC = 0x42505249;
    private static final int VERSION = 1;
    private static final int MAX_RULES_PER_POINT = 1_024;
    private static final int MAX_KEY_BYTES = 4_096;

    private BoundaryRouteIndexCodec() {
    }

    static byte[] encode(Collection<String> ruleKeys) {
        List<String> normalized = new ArrayList<>(new LinkedHashSet<>(ruleKeys));
        normalized.sort(Comparator.naturalOrder());
        if (normalized.size() > MAX_RULES_PER_POINT) {
            throw new IllegalArgumentException("point route exceeds the rule fan-out limit");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeInt(normalized.size());
                for (String ruleKey : normalized) {
                    if (ruleKey == null || ruleKey.isBlank()) {
                        throw new IllegalArgumentException("route rule key must be nonblank");
                    }
                    byte[] encoded = ruleKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    if (encoded.length > MAX_KEY_BYTES) {
                        throw new IllegalArgumentException("route rule key is too long");
                    }
                    output.writeInt(encoded.length);
                    output.write(encoded);
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode boundary route index", error);
        }
    }

    static List<String> decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported boundary route index header");
            }
            int size = input.readInt();
            if (size < 0 || size > MAX_RULES_PER_POINT) {
                throw new IOException("invalid boundary route fan-out: " + size);
            }
            List<String> result = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                int length = input.readInt();
                if (length <= 0 || length > MAX_KEY_BYTES) {
                    throw new IOException("invalid boundary route key length: " + length);
                }
                byte[] encoded = input.readNBytes(length);
                if (encoded.length != length) {
                    throw new IOException("truncated boundary route key");
                }
                result.add(new String(encoded, java.nio.charset.StandardCharsets.UTF_8));
            }
            if (input.read() != -1) {
                throw new IOException("boundary route index has trailing data");
            }
            return List.copyOf(result);
        } catch (IOException error) {
            throw new IllegalStateException("cannot decode boundary route index", error);
        }
    }
}
