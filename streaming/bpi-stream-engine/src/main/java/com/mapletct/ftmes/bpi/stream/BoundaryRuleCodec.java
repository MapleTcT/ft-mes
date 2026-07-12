package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryKind;
import com.mapletct.ftmes.bpi.rules.BoundaryRuleDefinition;
import com.mapletct.ftmes.bpi.rules.ConditionOperator;
import com.mapletct.ftmes.bpi.rules.EvidenceClass;
import com.mapletct.ftmes.bpi.rules.EvidenceCondition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class BoundaryRuleCodec {

    private static final int MAGIC = 0x42504952;
    private static final int VERSION = 1;
    private static final int MAX_CONDITIONS = 10_000;

    private BoundaryRuleCodec() {
    }

    public static byte[] encode(BoundaryRuleDefinition rule) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeUTF(rule.ruleCode());
                output.writeUTF(rule.ruleVersion());
                output.writeUTF(rule.boundaryKind().name());
                output.writeInt(rule.quorumMinimum());
                output.writeDouble(rule.minimumConfidence());
                output.writeDouble(rule.maxCompositePenalty());
                output.writeInt(rule.conditions().size());
                for (EvidenceCondition condition : rule.conditions()) {
                    output.writeUTF(condition.signal());
                    output.writeUTF(condition.operator().name());
                    writeNullable(output, condition.threshold() == null
                            ? null : condition.threshold().toPlainString());
                    output.writeLong(condition.holdFor().toMillis());
                    output.writeLong(condition.maxSilence().toMillis());
                    output.writeUTF(condition.classification().name());
                    output.writeInt(condition.weight());
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode boundary rule", error);
        }
    }

    public static BoundaryRuleDefinition decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            requireHeader(input, MAGIC, VERSION, "boundary rule");
            String ruleCode = input.readUTF();
            String ruleVersion = input.readUTF();
            BoundaryKind boundaryKind = BoundaryKind.valueOf(input.readUTF());
            int quorumMinimum = input.readInt();
            double minimumConfidence = input.readDouble();
            double maxCompositePenalty = input.readDouble();
            int conditionCount = boundedCount(input.readInt(), MAX_CONDITIONS, "condition");
            List<EvidenceCondition> conditions = new ArrayList<>(conditionCount);
            for (int index = 0; index < conditionCount; index++) {
                String signal = input.readUTF();
                ConditionOperator operator = ConditionOperator.valueOf(input.readUTF());
                String threshold = readNullable(input);
                conditions.add(new EvidenceCondition(
                        signal,
                        operator,
                        threshold == null ? null : new BigDecimal(threshold),
                        Duration.ofMillis(input.readLong()),
                        Duration.ofMillis(input.readLong()),
                        EvidenceClass.valueOf(input.readUTF()),
                        input.readInt()));
            }
            requireFullyRead(input, "boundary rule");
            return new BoundaryRuleDefinition(
                    ruleCode,
                    ruleVersion,
                    boundaryKind,
                    quorumMinimum,
                    minimumConfidence,
                    maxCompositePenalty,
                    conditions);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode boundary rule", error);
        }
    }

    static void requireHeader(DataInputStream input, int magic, int version, String type) throws IOException {
        if (input.readInt() != magic) {
            throw new IOException("invalid " + type + " magic");
        }
        int encodedVersion = input.readInt();
        if (encodedVersion != version) {
            throw new IOException("unsupported " + type + " version: " + encodedVersion);
        }
    }

    static int boundedCount(int count, int maximum, String type) throws IOException {
        if (count < 0 || count > maximum) {
            throw new IOException("invalid " + type + " count: " + count);
        }
        return count;
    }

    static void requireFullyRead(DataInputStream input, String type) throws IOException {
        if (input.available() != 0) {
            throw new IOException("trailing bytes in " + type);
        }
    }

    static void writeNullable(DataOutputStream output, String value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) {
            output.writeUTF(value);
        }
    }

    static String readNullable(DataInputStream input) throws IOException {
        return input.readBoolean() ? input.readUTF() : null;
    }
}
