package com.mapletct.ftmes.bpi.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class KafkaIngressRecordCodec {

    private static final int MAGIC = 0x42504B52;
    private static final int VERSION = 1;
    private static final int MAX_BYTES = 1_048_576;

    private KafkaIngressRecordCodec() {
    }

    public static byte[] encode(KafkaIngressRecord record) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeUTF(record.topic());
                output.writeInt(record.partition());
                output.writeLong(record.offset());
                output.writeLong(record.timestamp());
                writeBytes(output, record.key());
                writeBytes(output, record.value());
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode Kafka ingress record", error);
        }
    }

    public static KafkaIngressRecord decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("unsupported Kafka ingress record header");
            }
            KafkaIngressRecord record = new KafkaIngressRecord(
                    input.readUTF(), input.readInt(), input.readLong(), input.readLong(),
                    readBytes(input), readBytes(input));
            if (input.read() != -1) {
                throw new IOException("Kafka ingress record has trailing data");
            }
            return record;
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode Kafka ingress record", error);
        }
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        if (value.length > MAX_BYTES) {
            throw new IOException("Kafka ingress field exceeds one MiB");
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length == -1) {
            return null;
        }
        if (length < 0 || length > MAX_BYTES) {
            throw new IOException("invalid Kafka ingress field length: " + length);
        }
        byte[] value = input.readNBytes(length);
        if (value.length != length) {
            throw new IOException("truncated Kafka ingress field");
        }
        return value;
    }
}
