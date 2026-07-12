package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public final class BoundaryRuleKafkaDecodeFunction extends ProcessFunction<byte[], byte[]> {

    public static final OutputTag<KafkaDecodeIssue> ISSUES =
            new OutputTag<>("bpi-rule-kafka-decode-issues") {
            };

    @Override
    public void processElement(byte[] bytes, Context context, Collector<byte[]> output) {
        KafkaIngressRecord record;
        try {
            record = KafkaIngressRecordCodec.decode(bytes);
        } catch (IllegalStateException error) {
            context.output(ISSUES, new KafkaDecodeIssue(
                    "KAFKA_RECORD_REJECTED", "", -1, -1, "", error.getMessage()));
            return;
        }
        if (record.value() == null) {
            issue(context, record, "RULE_TOMBSTONE_REJECTED", "", "typed inactive publication is required");
            return;
        }
        BoundaryRulePublicationV1 publication;
        try {
            publication = BoundaryRulePublicationV1.parseFrom(record.value());
            BoundaryRulePublicationMapper.map(publication);
        } catch (InvalidProtocolBufferException error) {
            issue(context, record, "RULE_PROTOBUF_REJECTED", "", error.getMessage());
            return;
        } catch (IllegalArgumentException | IllegalStateException error) {
            issue(context, record, "RULE_CONTRACT_REJECTED", eventId(record.value()), error.getMessage());
            return;
        }
        output.collect(publication.toByteArray());
    }

    private static String eventId(byte[] value) {
        try {
            return BoundaryRulePublicationV1.parseFrom(value).getEventId();
        } catch (InvalidProtocolBufferException ignored) {
            return "";
        }
    }

    private static void issue(
            Context context,
            KafkaIngressRecord record,
            String code,
            String eventId,
            String detail) {
        context.output(ISSUES, new KafkaDecodeIssue(
                code, record.topic(), record.partition(), record.offset(), eventId, detail));
    }
}
