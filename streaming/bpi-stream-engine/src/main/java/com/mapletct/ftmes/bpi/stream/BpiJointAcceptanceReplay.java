package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.validation.BpiContractValidator;
import com.mapletct.ftmes.bpi.contract.v1.BatchCandidateV1;
import com.mapletct.ftmes.bpi.contract.v1.DataQualityEventV1;
import com.mapletct.ftmes.bpi.contract.v1.ProductionContextEventV1;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public final class BpiJointAcceptanceReplay {

    private BpiJointAcceptanceReplay() {
    }

    public static void main(String[] args) throws Exception {
        BpiJointAcceptanceReplayConfig config = BpiJointAcceptanceReplayConfig
                .fromEnvironment(System.getenv());
        BpiJointAcceptanceScenario.Scenario scenario = BpiJointAcceptanceScenario
                .create(config, Instant.now().minusSeconds(10));
        try {
            ReplayResult result = execute(config, scenario);
            writeReport(config, scenario, result, "PASS", null);
            System.out.println("BPI joint replay PASS marker=" + scenario.marker()
                    + " contextSource=" + config.contextSource()
                    + " candidateKey=" + result.candidate().getCandidateKey()
                    + " report=" + config.reportPath());
        } catch (Exception error) {
            writeReport(config, scenario, null, "FAIL", error.getMessage());
            throw error;
        }
    }

    static ReplayResult execute(
            BpiJointAcceptanceReplayConfig config,
            BpiJointAcceptanceScenario.Scenario scenario) throws Exception {
        List<InputOffset> inputs = new ArrayList<>();
        try (KafkaConsumer<byte[], byte[]> consumer = consumer(config);
             KafkaProducer<byte[], byte[]> producer = producer(config)) {
            boolean syntheticContextPublished = false;
            boolean syntheticContextClosed = false;
            positionAtEnd(consumer, config);
            try {
                if (config.usesMesOutboxContext()) {
                    inputs.add(awaitMesOutboxContext(config, scenario));
                } else {
                    inputs.add(sendContext(producer, config, scenario, scenario.context()));
                    syntheticContextPublished = true;
                }
                for (int index = 0; index < scenario.telemetry().size(); index++) {
                    TelemetryEnvelopeV1 telemetry = scenario.telemetry().get(index);
                    inputs.add(send(
                            producer,
                            config.telemetryTopic(),
                            scenario.lineId() + "|" + scenario.deviceId(),
                            telemetry.getEventTimeMs(),
                            telemetry.getEventId(),
                            scenario.tenantId(),
                            telemetry.toByteArray()));
                    if (index + 1 < scenario.telemetry().size()) {
                        sleep(config.telemetrySpacing());
                    }
                }
                producer.flush();
                ReplayResult observed = awaitResult(consumer, config, scenario, List.copyOf(inputs));
                if (syntheticContextPublished) {
                    inputs.add(sendContext(producer, config, scenario, scenario.closingContext()));
                    producer.flush();
                    syntheticContextClosed = true;
                    return withInputs(observed, inputs);
                }
                return observed;
            } catch (Exception failure) {
                if (syntheticContextPublished && !syntheticContextClosed) {
                    try {
                        sendContext(producer, config, scenario, scenario.closingContext());
                        producer.flush();
                    } catch (Exception cleanupFailure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            }
        }
    }

    private static InputOffset sendContext(
            KafkaProducer<byte[], byte[]> producer,
            BpiJointAcceptanceReplayConfig config,
            BpiJointAcceptanceScenario.Scenario scenario,
            ProductionContextEventV1 context) throws ExecutionException, InterruptedException {
        return send(
                producer,
                config.contextTopic(),
                scenario.contextKey(),
                context.getEffectiveFromMs(),
                context.getEventId(),
                scenario.tenantId(),
                context.toByteArray());
    }

    private static ReplayResult withInputs(ReplayResult observed, List<InputOffset> inputs) {
        return new ReplayResult(
                List.copyOf(inputs),
                observed.candidate(),
                observed.candidateOffset(),
                observed.candidateCount(),
                observed.qualityIssues());
    }

    static boolean matchesCandidate(
            BatchCandidateV1 candidate,
            BpiJointAcceptanceScenario.Scenario scenario) {
        return candidate.getTenantId().equals(scenario.tenantId())
                && candidate.getPlantId().equals(scenario.plantId())
                && candidate.getLineId().equals(scenario.lineId())
                && candidate.getRuleCode().equals(scenario.ruleCode())
                && candidate.getContextOrderId().equals(scenario.orderId());
    }

    static boolean matchesIssue(
            DataQualityEventV1 issue,
            BpiJointAcceptanceScenario.Scenario scenario) {
        return issue.getSourceEventId().contains(scenario.marker())
                || issue.getEventId().contains(scenario.marker());
    }

    static boolean matchesContext(
            ProductionContextEventV1 context,
            BpiJointAcceptanceScenario.Scenario scenario) {
        return sameContextIdentity(context, scenario) && context.getActive();
    }

    private static boolean sameContextIdentity(
            ProductionContextEventV1 context,
            BpiJointAcceptanceScenario.Scenario scenario) {
        return context.getTenantId().equals(scenario.tenantId())
                && context.getPlantId().equals(scenario.plantId())
                && context.getLineId().equals(scenario.lineId())
                && context.getOrderId().equals(scenario.orderId());
    }

    static boolean isNewerContext(
            ProductionContextEventV1 candidate,
            ProductionContextEventV1 current) {
        return Comparator.comparingLong(ProductionContextEventV1::getContextRevision)
                .thenComparingLong(ProductionContextEventV1::getEffectiveFromMs)
                .thenComparing(ProductionContextEventV1::getEventId)
                .compare(candidate, current) > 0;
    }

    private static InputOffset awaitMesOutboxContext(
            BpiJointAcceptanceReplayConfig config,
            BpiJointAcceptanceScenario.Scenario scenario) throws InvalidProtocolBufferException {
        try (KafkaConsumer<byte[], byte[]> consumer = contextConsumer(config)) {
            consumer.subscribe(List.of(config.contextTopic()));
            Instant assignmentDeadline = Instant.now().plusSeconds(20);
            while (consumer.assignment().isEmpty() && Instant.now().isBefore(assignmentDeadline)) {
                consumer.poll(Duration.ofMillis(250));
            }
            if (consumer.assignment().isEmpty()) {
                throw new IllegalStateException("Kafka MES context consumer received no partition assignment");
            }
            consumer.seekToBeginning(consumer.assignment());
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
            Instant deadline = Instant.now().plus(config.timeout());
            ContextInput latest = null;
            int stableEndPolls = 0;
            while (Instant.now().isBefore(deadline)) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    ProductionContextEventV1 context = ProductionContextEventV1.parseFrom(record.value());
                    if (sameContextIdentity(context, scenario)) {
                        List<?> violations = BpiContractValidator.validate(context);
                        if (!violations.isEmpty()) {
                            throw new IllegalStateException("matching MES context violates the BPI v1 contract");
                        }
                        if (latest == null || isNewerContext(context, latest.context())) {
                            latest = new ContextInput(record, context);
                        }
                    }
                }
                if (atEnd(consumer, endOffsets)) {
                    Map<TopicPartition, Long> refreshed = consumer.endOffsets(consumer.assignment());
                    if (refreshed.equals(endOffsets)) {
                        stableEndPolls++;
                        if (stableEndPolls >= 2) {
                            break;
                        }
                    } else {
                        endOffsets = refreshed;
                        stableEndPolls = 0;
                    }
                } else {
                    stableEndPolls = 0;
                }
            }
            if (latest == null) {
                throw new IllegalStateException("no MES outbox context matched order " + scenario.orderId());
            }
            if (!latest.context().getActive()) {
                throw new IllegalStateException("latest MES outbox context is inactive for order "
                        + scenario.orderId());
            }
            ConsumerRecord<byte[], byte[]> record = latest.record();
            return new InputOffset(
                    record.topic(), record.partition(), record.offset(), latest.context().getEventId());
        }
    }

    private static boolean atEnd(
            KafkaConsumer<byte[], byte[]> consumer,
            Map<TopicPartition, Long> endOffsets) {
        for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
            if (consumer.position(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static ReplayResult awaitResult(
            KafkaConsumer<byte[], byte[]> consumer,
            BpiJointAcceptanceReplayConfig config,
            BpiJointAcceptanceScenario.Scenario scenario,
            List<InputOffset> inputs) throws InvalidProtocolBufferException {
        Instant deadline = Instant.now().plus(config.timeout());
        Instant graceDeadline = null;
        BatchCandidateV1 matched = null;
        OutputOffset candidateOffset = null;
        int candidateCount = 0;
        List<String> qualityIssues = new ArrayList<>();

        while (Instant.now().isBefore(deadline)
                && (matched == null || Instant.now().isBefore(graceDeadline))) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (record.topic().equals(config.candidateTopic())) {
                    BatchCandidateV1 candidate = BatchCandidateV1.parseFrom(record.value());
                    if (matchesCandidate(candidate, scenario)) {
                        List<?> violations = BpiContractValidator.validate(candidate);
                        if (!violations.isEmpty()) {
                            throw new IllegalStateException("matching candidate violates the BPI v1 contract");
                        }
                        candidateCount++;
                        if (matched == null) {
                            matched = candidate;
                            candidateOffset = new OutputOffset(record.topic(), record.partition(), record.offset());
                            graceDeadline = Instant.now().plus(config.resultGrace());
                        }
                    }
                } else if (record.topic().equals(config.dataQualityTopic())) {
                    DataQualityEventV1 issue = DataQualityEventV1.parseFrom(record.value());
                    if (matchesIssue(issue, scenario)) {
                        qualityIssues.add(issue.getIssueCode() + ":" + issue.getDetail());
                    }
                }
            }
        }
        if (matched == null) {
            throw new IllegalStateException("no candidate from the browser-published rule arrived before timeout");
        }
        if (candidateCount != 1) {
            throw new IllegalStateException("expected exactly one matching candidate, found " + candidateCount);
        }
        if (!qualityIssues.isEmpty()) {
            throw new IllegalStateException("matching data-quality issues: " + qualityIssues);
        }
        return new ReplayResult(inputs, matched, candidateOffset, candidateCount, List.copyOf(qualityIssues));
    }

    private static void positionAtEnd(
            KafkaConsumer<byte[], byte[]> consumer,
            BpiJointAcceptanceReplayConfig config) {
        consumer.subscribe(List.of(config.candidateTopic(), config.dataQualityTopic()));
        Instant deadline = Instant.now().plusSeconds(20);
        while (consumer.assignment().isEmpty() && Instant.now().isBefore(deadline)) {
            consumer.poll(Duration.ofMillis(250));
        }
        if (consumer.assignment().isEmpty()) {
            throw new IllegalStateException("Kafka joint replay consumer received no partition assignment");
        }
        consumer.seekToEnd(consumer.assignment());
        for (TopicPartition partition : consumer.assignment()) {
            consumer.position(partition);
        }
    }

    private static KafkaProducer<byte[], byte[]> producer(BpiJointAcceptanceReplayConfig config) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "30000");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "ft-mes-bpi-joint-replay-" + config.marker());
        return new KafkaProducer<>(properties);
    }

    private static KafkaConsumer<byte[], byte[]> consumer(BpiJointAcceptanceReplayConfig config) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, config.consumerGroup());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        return new KafkaConsumer<>(properties);
    }

    private static KafkaConsumer<byte[], byte[]> contextConsumer(BpiJointAcceptanceReplayConfig config) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, config.consumerGroup() + "-mes-context");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        return new KafkaConsumer<>(properties);
    }

    private static InputOffset send(
            KafkaProducer<byte[], byte[]> producer,
            String topic,
            String key,
            long timestamp,
            String eventId,
            String tenantId,
            byte[] payload) throws ExecutionException, InterruptedException {
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
        return new InputOffset(topic, metadata.partition(), metadata.offset(), eventId);
    }

    private static void sleep(Duration duration) throws InterruptedException {
        if (!duration.isZero()) {
            Thread.sleep(duration.toMillis());
        }
    }

    static void writeReport(
            BpiJointAcceptanceReplayConfig config,
            BpiJointAcceptanceScenario.Scenario scenario,
            ReplayResult result,
            String status,
            String error) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"generatedAt\": ").append(quote(Instant.now().toString())).append(",\n")
                .append("  \"status\": ").append(quote(status)).append(",\n")
                .append("  \"marker\": ").append(quote(scenario.marker())).append(",\n")
                .append("  \"ruleSource\": \"BPI_BROWSER_PUBLICATION_OUTBOX\",\n")
                .append("  \"contextSource\": ").append(quote(config.contextSource())).append(",\n")
                .append("  \"scope\": {\"tenantId\": ").append(quote(scenario.tenantId()))
                .append(", \"plantId\": ").append(quote(scenario.plantId()))
                .append(", \"lineId\": ").append(quote(scenario.lineId()))
                .append(", \"rule\": ").append(quote(config.ruleCode() + "@" + config.ruleVersion()))
                .append("},\n")
                .append("  \"topics\": {\n")
                .append("    \"telemetry\": ").append(quote(config.telemetryTopic())).append(",\n")
                .append("    \"context\": ").append(quote(config.contextTopic())).append(",\n")
                .append("    \"candidate\": ").append(quote(config.candidateTopic())).append(",\n")
                .append("    \"dataQuality\": ").append(quote(config.dataQualityTopic())).append("\n")
                .append("  },\n");
        if (result != null) {
            json.append("  \"inputs\": [\n");
            for (int index = 0; index < result.inputs().size(); index++) {
                InputOffset input = result.inputs().get(index);
                json.append("    {\"topic\": ").append(quote(input.topic()))
                        .append(", \"partition\": ").append(input.partition())
                        .append(", \"offset\": ").append(input.offset())
                        .append(", \"eventId\": ").append(quote(input.eventId())).append("}");
                json.append(index + 1 == result.inputs().size() ? "\n" : ",\n");
            }
            json.append("  ],\n")
                    .append("  \"candidate\": {\n")
                    .append("    \"candidateKey\": ").append(quote(result.candidate().getCandidateKey())).append(",\n")
                    .append("    \"eventId\": ").append(quote(result.candidate().getEventId())).append(",\n")
                    .append("    \"topic\": ").append(quote(result.candidateOffset().topic())).append(",\n")
                    .append("    \"partition\": ").append(result.candidateOffset().partition()).append(",\n")
                    .append("    \"offset\": ").append(result.candidateOffset().offset()).append(",\n")
                    .append("    \"matchingRecordCount\": ").append(result.candidateCount()).append(",\n")
                    .append("    \"evidenceEventIds\": [");
            for (int index = 0; index < result.candidate().getEvidenceEventIdsCount(); index++) {
                if (index > 0) json.append(", ");
                json.append(quote(result.candidate().getEvidenceEventIds(index)));
            }
            json.append("]\n  },\n")
                    .append("  \"matchingDataQualityIssues\": ").append(result.qualityIssues().size()).append(",\n");
        }
        json.append("  \"error\": ").append(error == null ? "null" : quote(error)).append("\n}");
        Files.createDirectories(config.reportPath().getParent());
        Files.writeString(config.reportPath(), json + "\n", StandardCharsets.UTF_8);
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

    record InputOffset(String topic, int partition, long offset, String eventId) {
    }

    record OutputOffset(String topic, int partition, long offset) {
    }

    private record ContextInput(
            ConsumerRecord<byte[], byte[]> record,
            ProductionContextEventV1 context) {
    }

    record ReplayResult(
            List<InputOffset> inputs,
            BatchCandidateV1 candidate,
            OutputOffset candidateOffset,
            int candidateCount,
            List<String> qualityIssues) {
    }
}
