package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.rules.BoundaryWindowState;
import com.mapletct.ftmes.bpi.rules.ConditionStatus;
import com.mapletct.ftmes.bpi.rules.EvidenceSignalState;
import com.mapletct.ftmes.bpi.rules.SignalQuality;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BoundaryOperatorStateCodec {

    private static final int MAGIC = 0x42504953;
    private static final int VERSION = 1;
    private static final int MAX_SIGNALS = 100_000;

    private BoundaryOperatorStateCodec() {
    }

    public static byte[] encode(BoundaryOperatorState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                writeContext(output, state.context());
                output.writeUTF(state.ruleRef().ruleCode());
                output.writeUTF(state.ruleRef().ruleVersion());
                output.writeLong(state.nextTimerEpochMs());
                BoundaryWindowState window = state.windowState();
                output.writeBoolean(window.candidateEmitted());
                BoundaryRuleCodec.writeNullable(output, window.firstQuorumEvidenceEventId());
                List<String> signals = new ArrayList<>(window.signals().keySet());
                signals.sort(String::compareTo);
                output.writeInt(signals.size());
                for (String signal : signals) {
                    writeSignal(output, window.signals().get(signal));
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("cannot encode boundary operator state", error);
        }
    }

    public static BoundaryOperatorState decode(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            BoundaryRuleCodec.requireHeader(input, MAGIC, VERSION, "boundary operator state");
            BoundaryExecutionContext context = readContext(input);
            BoundaryRuleRef ruleRef = new BoundaryRuleRef(input.readUTF(), input.readUTF());
            long nextTimer = input.readLong();
            boolean candidateEmitted = input.readBoolean();
            String firstQuorumEvent = BoundaryRuleCodec.readNullable(input);
            int signalCount = BoundaryRuleCodec.boundedCount(input.readInt(), MAX_SIGNALS, "signal");
            Map<String, EvidenceSignalState> signals = new HashMap<>();
            for (int index = 0; index < signalCount; index++) {
                EvidenceSignalState signal = readSignal(input);
                if (signals.put(signal.signal(), signal) != null) {
                    throw new IOException("duplicate signal in boundary operator state: " + signal.signal());
                }
            }
            BoundaryRuleCodec.requireFullyRead(input, "boundary operator state");
            return new BoundaryOperatorState(
                    context,
                    ruleRef,
                    new BoundaryWindowState(signals, candidateEmitted, firstQuorumEvent),
                    nextTimer);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalStateException("cannot decode boundary operator state", error);
        }
    }

    private static void writeContext(DataOutputStream output, BoundaryExecutionContext context) throws IOException {
        output.writeUTF(context.tenantId());
        output.writeUTF(context.plantId());
        output.writeUTF(context.lineId());
        output.writeUTF(context.localityGroup());
        output.writeUTF(context.topologyCode());
        output.writeUTF(context.topologyVersion());
        BoundaryRuleCodec.writeNullable(output, context.contextOrderId());
        BoundaryRuleCodec.writeNullable(output, context.batchId());
    }

    private static BoundaryExecutionContext readContext(DataInputStream input) throws IOException {
        return new BoundaryExecutionContext(
                input.readUTF(),
                input.readUTF(),
                input.readUTF(),
                input.readUTF(),
                input.readUTF(),
                input.readUTF(),
                BoundaryRuleCodec.readNullable(input),
                BoundaryRuleCodec.readNullable(input));
    }

    private static void writeSignal(DataOutputStream output, EvidenceSignalState state) throws IOException {
        output.writeUTF(state.signal());
        output.writeUTF(state.status().name());
        writeInstant(output, state.trueSince());
        BoundaryRuleCodec.writeNullable(output, state.firstTrueEventId());
        BoundaryRuleCodec.writeNullable(output, state.lastEventId());
        writeInstant(output, state.lastEventTime());
        writeDecimal(output, state.previousNumericValue());
        writeDecimal(output, state.currentNumericValue());
        writeBoolean(output, state.currentBooleanValue());
        output.writeUTF(state.quality().name());
    }

    private static EvidenceSignalState readSignal(DataInputStream input) throws IOException {
        return new EvidenceSignalState(
                input.readUTF(),
                ConditionStatus.valueOf(input.readUTF()),
                readInstant(input),
                BoundaryRuleCodec.readNullable(input),
                BoundaryRuleCodec.readNullable(input),
                readInstant(input),
                readDecimal(input),
                readDecimal(input),
                readBoolean(input),
                SignalQuality.valueOf(input.readUTF()));
    }

    private static void writeInstant(DataOutputStream output, Instant value) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) {
            output.writeLong(value.getEpochSecond());
            output.writeInt(value.getNano());
        }
    }

    private static Instant readInstant(DataInputStream input) throws IOException {
        return input.readBoolean() ? Instant.ofEpochSecond(input.readLong(), input.readInt()) : null;
    }

    private static void writeDecimal(DataOutputStream output, BigDecimal value) throws IOException {
        BoundaryRuleCodec.writeNullable(output, value == null ? null : value.toPlainString());
    }

    private static BigDecimal readDecimal(DataInputStream input) throws IOException {
        String value = BoundaryRuleCodec.readNullable(input);
        return value == null ? null : new BigDecimal(value);
    }

    private static void writeBoolean(DataOutputStream output, Boolean value) throws IOException {
        output.writeByte(value == null ? -1 : value ? 1 : 0);
    }

    private static Boolean readBoolean(DataInputStream input) throws IOException {
        int value = input.readByte();
        if (value == -1) {
            return null;
        }
        if (value == 0) {
            return false;
        }
        if (value == 1) {
            return true;
        }
        throw new IOException("invalid nullable boolean value: " + value);
    }
}
