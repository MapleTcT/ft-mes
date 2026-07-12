package com.mapletct.ftmes.bpi.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class BoundaryRuleUpdateCodec {

    private static final int MAGIC = 0x42505255;
    private static final int VERSION = 1;
    private static final int MAX_STRING_BYTES = 4_096;
    private static final int MAX_RULE_BYTES = 1_048_576;

    private BoundaryRuleUpdateCodec() {
    }

    public static byte[] encode(BoundaryRuleUpdate update) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeByte(update.operation().ordinal());
                writeString(output, update.ruleRef().tenantId());
                writeString(output, update.ruleRef().plantId());
                writeString(output, update.ruleRef().lineId());
                writeString(output, update.ruleRef().ruleCode());
                writeString(output, update.ruleRef().ruleVersion());
                if (update.operation() == BoundaryRuleUpdate.Operation.UPSERT) {
                    writeBytes(output, BoundaryRuleCodec.encode(update.rule()));
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode boundary rule update", error);
        }
    }

    public static BoundaryRuleUpdate decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported boundary rule update header");
            }
            int operationIndex = input.readUnsignedByte();
            if (operationIndex >= BoundaryRuleUpdate.Operation.values().length) {
                throw new IOException("unsupported boundary rule update operation");
            }
            BoundaryRuleUpdate.Operation operation = BoundaryRuleUpdate.Operation.values()[operationIndex];
            BoundaryRuleRef ruleRef = new BoundaryRuleRef(
                    readString(input),
                    readString(input),
                    readString(input),
                    readString(input),
                    readString(input));
            BoundaryRuleUpdate update = operation == BoundaryRuleUpdate.Operation.UPSERT
                    ? new BoundaryRuleUpdate(operation, ruleRef, BoundaryRuleCodec.decode(readBytes(input)))
                    : new BoundaryRuleUpdate(operation, ruleRef, null);
            if (input.read() != -1) {
                throw new IOException("boundary rule update has trailing data");
            }
            return update;
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode boundary rule update", error);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IOException("boundary rule update string is too long");
        }
        output.writeInt(encoded.length);
        output.write(encoded);
    }

    private static String readString(DataInputStream input) throws IOException {
        return new String(readBytes(input, MAX_STRING_BYTES), StandardCharsets.UTF_8);
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        if (value.length > MAX_RULE_BYTES) {
            throw new IOException("boundary rule update exceeds one MiB");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        return readBytes(input, MAX_RULE_BYTES);
    }

    private static byte[] readBytes(DataInputStream input, int maximum) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > maximum) {
            throw new IOException("invalid boundary rule update field length: " + length);
        }
        byte[] value = input.readNBytes(length);
        if (value.length != length) {
            throw new IOException("truncated boundary rule update");
        }
        return value;
    }
}
