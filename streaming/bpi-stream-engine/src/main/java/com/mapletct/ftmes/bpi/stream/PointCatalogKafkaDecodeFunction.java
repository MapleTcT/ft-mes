package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.PointCatalogSnapshotV1;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public final class PointCatalogKafkaDecodeFunction extends ProcessFunction<byte[], byte[]> {

    public static final OutputTag<KafkaDecodeIssue> ISSUES =
            new OutputTag<>("bpi-point-catalog-kafka-decode-issues") {
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
            issue(context, record, "POINT_CATALOG_TOMBSTONE_REJECTED", "", "typed snapshot is required");
            return;
        }
        try {
            PointCatalogSnapshotV1 snapshot = PointCatalogRuntimeValidator.validate(
                    PointCatalogSnapshotV1.parseFrom(record.value()));
            output.collect(snapshot.toByteArray());
        } catch (InvalidProtocolBufferException error) {
            issue(context, record, "POINT_CATALOG_PROTOBUF_REJECTED", "", error.getMessage());
        } catch (IllegalArgumentException error) {
            issue(context, record, "POINT_CATALOG_CONTRACT_REJECTED", eventId(record.value()), error.getMessage());
        }
    }

    private static String eventId(byte[] value) {
        try {
            return PointCatalogSnapshotV1.parseFrom(value).getEventId();
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
