package com.mapletct.ftmes.bpi.stream;

import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.PointValue;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
import com.mapletct.ftmes.bpi.contract.v1.SequenceOrigin;
import com.mapletct.ftmes.bpi.contract.v1.TelemetryEnvelopeV1;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class BpiDataQualityFlinkReplay {

    private static final Set<String> EXPECTED_CODES = Set.of(
            "SOURCE_SEQUENCE_GAP",
            "CLOCK_DRIFT",
            "POINT_QUALITY_BAD",
            "SOURCE_SEQUENCE_DUPLICATE");

    private BpiDataQualityFlinkReplay() {
    }

    public static void main(String[] args) throws Exception {
        BpiDataQualityFlinkReplayConfig config = BpiDataQualityFlinkReplayConfig
                .fromEnvironment(System.getenv());
        Scenario scenario = scenario(
                config.marker(), config.tenantId(), config.plantId(), config.lineId(),
                Instant.now().minusSeconds(2));
        try {
            ReplayResult result = execute(config, scenario);
            writeReport(config, scenario, result, "PASS", null);
            System.out.println("BPI Flink data-quality replay PASS marker=" + scenario.marker()
                    + " events=" + result.events().size() + " report=" + config.reportPath());
        } catch (Exception error) {
            writeReport(config, scenario, null, "FAIL", error.getMessage());
            throw error;
        }
    }

    static ReplayResult execute(
            BpiDataQualityFlinkReplayConfig config,
            Scenario scenario) throws Exception {
        List<LocatedInput> inputs = new ArrayList<>();
        try (KafkaConsumer<byte[], byte[]> consumer = consumer(config);
             KafkaProducer<byte[], byte[]> producer = producer(config)) {
            inputs.add(sendContext(producer, config.contextTopic(), scenario.activeContext()));
            List<LocatedEvent> events;
            LocatedInput cleanup;
            try {
                producer.flush();
                sleep(config.contextSettle());
                for (TelemetryEnvelopeV1 telemetry : scenario.telemetry()) {
                    inputs.add(sendTelemetry(producer, config.telemetryTopic(), telemetry));
                    producer.flush();
                    sleep(config.telemetrySpacing());
                }
                events = awaitEvents(consumer, config, scenario);
            } finally {
                cleanup = sendContext(producer, config.contextTopic(), scenario.inactiveContext());
                producer.flush();
            }
            return new ReplayResult(List.copyOf(inputs), events, cleanup);
        }
    }

    static Scenario scenario(String marker, Instant baseTime) {
        return scenario(marker, "TENANT-E2E", "PLANT-E2E", "LINE-" + marker, baseTime);
    }

    static Scenario scenario(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            Instant baseTime) {
        String gatewayId = "GATEWAY-" + marker;
        String deviceId = "DEVICE-" + marker;
        long contextRevision = baseTime.getEpochSecond();
        ProductionContextEventV1 active = ProductionContextEventV1.newBuilder()
                .setEventId(marker + "-CONTEXT-ACTIVE")
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setOrderId("MO-" + marker)
                .setTaskId("TASK-" + marker)
                .setMaterialCode("MATERIAL-E2E")
                .setRecipeVersion("RECIPE-E2E-1")
                .setEffectiveFromMs(baseTime.minus(Duration.ofMinutes(20)).toEpochMilli())
                .setContextRevision(contextRevision)
                .setActive(true)
                .putAttributes("acceptance_marker", marker)
                .build();
        ProductionContextEventV1 inactive = active.toBuilder()
                .setEventId(marker + "-CONTEXT-INACTIVE")
                .setEffectiveFromMs(baseTime.plusSeconds(5).toEpochMilli())
                .setContextRevision(contextRevision + 1)
                .setActive(false)
                .build();
        TelemetryEnvelopeV1 baseline = telemetry(
                marker + "-BASELINE", tenantId, plantId, lineId, gatewayId, deviceId,
                baseTime, baseTime.plusMillis(50), 100, "GOOD");
        TelemetryEnvelopeV1 fault = telemetry(
                marker + "-FAULT", tenantId, plantId, lineId, gatewayId, deviceId,
                baseTime.minus(Duration.ofMinutes(10)), baseTime.plusMillis(100), 102, "BAD");
        TelemetryEnvelopeV1 duplicate = telemetry(
                marker + "-DUPLICATE", tenantId, plantId, lineId, gatewayId, deviceId,
                baseTime.plusSeconds(1), baseTime.plusSeconds(1).plusMillis(50), 103, "GOOD");
        return new Scenario(
                marker, tenantId, plantId, lineId, gatewayId, deviceId, active, inactive,
                List.of(baseline, fault, duplicate, duplicate));
    }

    private static TelemetryEnvelopeV1 telemetry(
            String eventId,
            String tenantId,
            String plantId,
            String lineId,
            String gatewayId,
            String deviceId,
            Instant eventTime,
            Instant ingestTime,
            long sequence,
            String qualityCode) {
        return TelemetryEnvelopeV1.newBuilder()
                .setEventId(eventId)
                .setMessageId("MESSAGE-" + eventId)
                .setTenantId(tenantId)
                .setPlantId(plantId)
                .setLineId(lineId)
                .setGatewayId(gatewayId)
                .setProductId("PRODUCT-E2E")
                .setDeviceId(deviceId)
                .setEventTimeMs(eventTime.toEpochMilli())
                .setIngestTimeMs(ingestTime.toEpochMilli())
                .setSourceEpoch(1)
                .setSequence(sequence)
                .setSequenceOrigin(SequenceOrigin.GATEWAY)
                .addPoints(PointValue.newBuilder()
                        .setPropertyId("flow")
                        .setDoubleValue(3.5)
                        .setUnit("m3/h")
                        .setQualityCode(qualityCode)
                        .setSampleTimeMs(eventTime.toEpochMilli())
                        .setCalibrationVersion("CAL-E2E-1"))
                .putHeaders("acceptance_marker", eventId.substring(0, eventId.lastIndexOf('-')))
                .build();
    }

    private static List<LocatedEvent> awaitEvents(
            KafkaConsumer<byte[], byte[]> consumer,
            BpiDataQualityFlinkReplayConfig config,
            Scenario scenario) throws Exception {
        Instant deadline = Instant.now().plus(config.timeout());
        Instant graceDeadline = null;
        Map<String, LocatedEvent> matched = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        while (Instant.now().isBefore(deadline)
                && (matched.size() < EXPECTED_CODES.size()
                || graceDeadline == null
                || Instant.now().isBefore(graceDeadline))) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                DataQualityEventV1 event = DataQualityEventV1.parseFrom(record.value());
                if (!matchesScenario(event, scenario)) {
                    continue;
                }
                counts.merge(event.getIssueCode(), 1, Integer::sum);
                if (EXPECTED_CODES.contains(event.getIssueCode())) {
                    matched.putIfAbsent(event.getIssueCode(), new LocatedEvent(
                            event, record.topic(), record.partition(), record.offset()));
                    if (matched.size() == EXPECTED_CODES.size() && graceDeadline == null) {
                        graceDeadline = Instant.now().plus(config.resultGrace());
                    }
                }
            }
        }
        if (!matched.keySet().equals(EXPECTED_CODES)) {
            throw new IllegalStateException("expected Flink data-quality codes " + EXPECTED_CODES
                    + " but received " + matched.keySet());
        }
        if (!counts.keySet().equals(EXPECTED_CODES)
                || counts.values().stream().anyMatch(count -> count != 1)) {
            throw new IllegalStateException("matching data-quality records are not exactly once: " + counts);
        }
        if (matched.values().stream().anyMatch(item ->
                !"telemetry-data-quality".equals(item.event().getHeadersOrDefault("stage", "")))) {
            throw new IllegalStateException("matching event was not produced by the Flink telemetry detector");
        }
        return List.copyOf(matched.values());
    }

    static boolean matchesScenario(DataQualityEventV1 event, Scenario scenario) {
        return event.getTenantId().equals(scenario.tenantId())
                && event.getPlantId().equals(scenario.plantId())
                && event.getLineId().equals(scenario.lineId())
                && event.getSourceEventId().startsWith(scenario.marker() + "-");
    }

    private static void positionAtEnd(
            KafkaConsumer<byte[], byte[]> consumer,
            String topic) {
        consumer.subscribe(List.of(topic));
        Instant deadline = Instant.now().plusSeconds(20);
        while (consumer.assignment().isEmpty() && Instant.now().isBefore(deadline)) {
            consumer.poll(Duration.ofMillis(250));
        }
        if (consumer.assignment().isEmpty()) {
            throw new IllegalStateException("Kafka data-quality replay consumer received no partition assignment");
        }
        consumer.seekToEnd(consumer.assignment());
        for (TopicPartition partition : consumer.assignment()) {
            consumer.position(partition);
        }
    }

    private static KafkaProducer<byte[], byte[]> producer(BpiDataQualityFlinkReplayConfig config) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "30000");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "ft-mes-bpi-dq-replay-" + config.marker());
        return new KafkaProducer<>(properties);
    }

    private static KafkaConsumer<byte[], byte[]> consumer(BpiDataQualityFlinkReplayConfig config) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, config.consumerGroup());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties);
        try {
            positionAtEnd(consumer, config.dataQualityTopic());
            return consumer;
        } catch (RuntimeException error) {
            consumer.close();
            throw error;
        }
    }

    private static LocatedInput sendTelemetry(
            KafkaProducer<byte[], byte[]> producer,
            String topic,
            TelemetryEnvelopeV1 telemetry) throws Exception {
        return send(
                producer,
                topic,
                telemetry.getLineId() + "|" + telemetry.getDeviceId(),
                telemetry.getEventTimeMs(),
                telemetry.getEventId(),
                telemetry.getTenantId(),
                telemetry.toByteArray());
    }

    private static LocatedInput sendContext(
            KafkaProducer<byte[], byte[]> producer,
            String topic,
            ProductionContextEventV1 context) throws Exception {
        return send(
                producer,
                topic,
                String.join("|", context.getTenantId(), context.getPlantId(), context.getLineId()),
                context.getEffectiveFromMs(),
                context.getEventId(),
                context.getTenantId(),
                context.toByteArray());
    }

    private static LocatedInput send(
            KafkaProducer<byte[], byte[]> producer,
            String topic,
            String key,
            long timestamp,
            String eventId,
            String tenantId,
            byte[] payload) throws Exception {
        Headers headers = new RecordHeaders()
                .add("event_id", eventId.getBytes(StandardCharsets.UTF_8))
                .add("tenant_id", tenantId.getBytes(StandardCharsets.UTF_8))
                .add("schema_version", "v1".getBytes(StandardCharsets.UTF_8));
        RecordMetadata metadata = producer.send(new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key.getBytes(StandardCharsets.UTF_8),
                payload,
                headers)).get();
        return new LocatedInput(topic, metadata.partition(), metadata.offset(), eventId);
    }

    private static void sleep(Duration duration) throws InterruptedException {
        if (!duration.isZero()) {
            Thread.sleep(duration.toMillis());
        }
    }

    private static void writeReport(
            BpiDataQualityFlinkReplayConfig config,
            Scenario scenario,
            ReplayResult result,
            String status,
            String error) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"generatedAt\": ").append(quote(Instant.now().toString())).append(",\n")
                .append("  \"status\": ").append(quote(status)).append(",\n")
                .append("  \"marker\": ").append(quote(scenario.marker())).append(",\n")
                .append("  \"scope\": {\"tenantId\": ").append(quote(scenario.tenantId()))
                .append(", \"plantId\": ").append(quote(scenario.plantId()))
                .append(", \"lineId\": ").append(quote(scenario.lineId())).append("},\n")
                .append("  \"producer\": \"Flink telemetry-data-quality operator\",\n")
                .append("  \"expectedCodes\": [");
        int codeIndex = 0;
        for (String code : EXPECTED_CODES.stream().sorted().toList()) {
            if (codeIndex++ > 0) json.append(", ");
            json.append(quote(code));
        }
        json.append("],\n");
        if (result != null) {
            json.append("  \"inputs\": [\n");
            for (int index = 0; index < result.inputs().size(); index++) {
                LocatedInput input = result.inputs().get(index);
                json.append("    {\"topic\": ").append(quote(input.topic()))
                        .append(", \"partition\": ").append(input.partition())
                        .append(", \"offset\": ").append(input.offset())
                        .append(", \"eventId\": ").append(quote(input.eventId())).append("}")
                        .append(index + 1 == result.inputs().size() ? "\n" : ",\n");
            }
            json.append("  ],\n  \"outputs\": [\n");
            for (int index = 0; index < result.events().size(); index++) {
                LocatedEvent item = result.events().get(index);
                json.append("    {\"topic\": ").append(quote(item.topic()))
                        .append(", \"partition\": ").append(item.partition())
                        .append(", \"offset\": ").append(item.offset())
                        .append(", \"eventId\": ").append(quote(item.event().getEventId()))
                        .append(", \"sourceEventId\": ").append(quote(item.event().getSourceEventId()))
                        .append(", \"issueCode\": ").append(quote(item.event().getIssueCode()))
                        .append(", \"severity\": ").append(quote(item.event().getSeverity().name()))
                        .append("}").append(index + 1 == result.events().size() ? "\n" : ",\n");
            }
            json.append("  ],\n  \"cleanupContext\": {\"topic\": ")
                    .append(quote(result.cleanup().topic()))
                    .append(", \"partition\": ").append(result.cleanup().partition())
                    .append(", \"offset\": ").append(result.cleanup().offset())
                    .append(", \"eventId\": ").append(quote(result.cleanup().eventId())).append("},\n");
        }
        json.append("  \"error\": ").append(error == null ? "null" : quote(error)).append("\n}\n");
        Files.createDirectories(config.reportPath().getParent());
        Files.writeString(config.reportPath(), json, StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char item = value.charAt(index);
            switch (item) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(item < 0x20 ? String.format("\\u%04x", (int) item) : item);
            }
        }
        return escaped.append('"').toString();
    }

    record Scenario(
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String gatewayId,
            String deviceId,
            ProductionContextEventV1 activeContext,
            ProductionContextEventV1 inactiveContext,
            List<TelemetryEnvelopeV1> telemetry) {
    }

    record LocatedInput(String topic, int partition, long offset, String eventId) {
    }

    record LocatedEvent(DataQualityEventV1 event, String topic, int partition, long offset) {
    }

    record ReplayResult(List<LocatedInput> inputs, List<LocatedEvent> events, LocatedInput cleanup) {
    }
}
