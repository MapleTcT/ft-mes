package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public final class BpiRuleDeactivationReplay {

    private BpiRuleDeactivationReplay() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.fromEnvironment(System.getenv());
        try {
            Result result = execute(config);
            writeReport(config, result, "PASS", null);
            System.out.println("BPI rule deactivation PASS marker=" + config.marker()
                    + " publicationEventId=" + result.publication().getEventId()
                    + " report=" + config.reportPath());
        } catch (Exception error) {
            writeReport(config, null, "FAIL", error.getMessage());
            throw error;
        }
    }

    static Result execute(Config config) throws Exception {
        try (KafkaConsumer<byte[], byte[]> ruleConsumer = consumer(
                    config, "rule-scan", "earliest");
             KafkaConsumer<byte[], byte[]> applicationConsumer = consumer(
                    config, "application", "latest");
             KafkaProducer<byte[], byte[]> producer = producer(config)) {
            positionAtEnd(applicationConsumer, config.applicationTopic());
            LocatedPublication located = findPublication(ruleConsumer, config);
            BoundaryRulePublicationV1 inactive = inactive(located.publication(), config.marker(), Instant.now());
            RecordMetadata output = producer.send(new ProducerRecord<>(
                    config.ruleTopic(),
                    null,
                    inactive.getPublishedAtMs(),
                    located.key(),
                    inactive.toByteArray(),
                    headers(located, config.marker()))).get();
            producer.flush();
            LocatedApplication application = awaitApplication(applicationConsumer, config, inactive.getEventId());
            return new Result(located, inactive, output, application);
        }
    }

    static BoundaryRulePublicationV1 inactive(
            BoundaryRulePublicationV1 active,
            String marker,
            Instant publishedAt) {
        long timestamp = Math.max(publishedAt.toEpochMilli(), active.getPublishedAtMs() + 1);
        return active.toBuilder()
                .setActive(false)
                .setPublishedAtMs(timestamp)
                .putHeaders("acceptance_cleanup", marker)
                .build();
    }

    static boolean matches(BoundaryRulePublicationV1 publication, Config config) {
        return publication.getTenantId().equals(config.tenantId())
                && publication.getPlantId().equals(config.plantId())
                && publication.getLineId().equals(config.lineId())
                && publication.getRuleCode().equals(config.ruleCode())
                && publication.getRuleVersion().equals(config.ruleVersion());
    }

    private static LocatedPublication findPublication(
            KafkaConsumer<byte[], byte[]> consumer,
            Config config) throws InvalidProtocolBufferException {
        List<TopicPartition> partitions = consumer.partitionsFor(config.ruleTopic()).stream()
                .map(info -> new TopicPartition(info.topic(), info.partition()))
                .toList();
        if (partitions.isEmpty()) {
            throw new IllegalStateException("rule topic has no partitions");
        }
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
        Instant deadline = Instant.now().plus(config.timeout());
        List<LocatedPublication> matches = new ArrayList<>();
        while (Instant.now().isBefore(deadline) && !atEnd(consumer, endOffsets)) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (record.value() == null) continue;
                BoundaryRulePublicationV1 publication = BoundaryRulePublicationV1.parseFrom(record.value());
                if (matches(publication, config) && publication.getActive()) {
                    matches.add(new LocatedPublication(
                            record.key(), new RecordHeaders(record.headers()), publication,
                            record.partition(), record.offset()));
                }
            }
        }
        return matches.stream()
                .max(Comparator.comparingLong(value -> value.publication().getPublishedAtMs()))
                .orElseThrow(() -> new IllegalStateException("active browser-published rule was not found"));
    }

    private static boolean atEnd(
            KafkaConsumer<byte[], byte[]> consumer,
            Map<TopicPartition, Long> endOffsets) {
        for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
            if (consumer.position(entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    private static LocatedApplication awaitApplication(
            KafkaConsumer<byte[], byte[]> consumer,
            Config config,
            String publicationEventId) throws InvalidProtocolBufferException {
        Instant deadline = Instant.now().plus(config.timeout());
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                BoundaryRuleApplicationV1 application = BoundaryRuleApplicationV1.parseFrom(record.value());
                if (application.getPublicationEventId().equals(publicationEventId)
                        && application.getTenantId().equals(config.tenantId())
                        && application.getPlantId().equals(config.plantId())
                        && application.getLineId().equals(config.lineId())
                        && application.getRuleCode().equals(config.ruleCode())
                        && application.getRuleVersion().equals(config.ruleVersion())
                        && application.getStatus() == BoundaryRuleApplicationStatusV1.APPLIED) {
                    return new LocatedApplication(application, record.partition(), record.offset());
                }
            }
        }
        throw new IllegalStateException("Flink did not acknowledge the typed inactive publication");
    }

    private static RecordHeaders headers(LocatedPublication located, String marker) {
        RecordHeaders result = new RecordHeaders(located.headers());
        result.add("acceptance_cleanup", marker.getBytes(StandardCharsets.UTF_8));
        return result;
    }

    private static void positionAtEnd(KafkaConsumer<byte[], byte[]> consumer, String topic) {
        consumer.subscribe(List.of(topic));
        Instant deadline = Instant.now().plusSeconds(20);
        while (consumer.assignment().isEmpty() && Instant.now().isBefore(deadline)) {
            consumer.poll(Duration.ofMillis(250));
        }
        if (consumer.assignment().isEmpty()) {
            throw new IllegalStateException("application consumer received no partition assignment");
        }
        consumer.seekToEnd(consumer.assignment());
        for (TopicPartition partition : consumer.assignment()) consumer.position(partition);
    }

    private static KafkaProducer<byte[], byte[]> producer(Config config) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "30000");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "ft-mes-bpi-rule-cleanup-" + config.marker());
        return new KafkaProducer<>(properties);
    }

    private static KafkaConsumer<byte[], byte[]> consumer(
            Config config,
            String purpose,
            String offsetReset) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "ft-mes-bpi-rule-cleanup-" + purpose + "-" + config.marker());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        return new KafkaConsumer<>(properties);
    }

    static void writeReport(Config config, Result result, String status, String error) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"generatedAt\": ").append(quote(Instant.now().toString())).append(",\n")
                .append("  \"status\": ").append(quote(status)).append(",\n")
                .append("  \"marker\": ").append(quote(config.marker())).append(",\n")
                .append("  \"scope\": {\"tenantId\": ").append(quote(config.tenantId()))
                .append(", \"plantId\": ").append(quote(config.plantId()))
                .append(", \"lineId\": ").append(quote(config.lineId()))
                .append(", \"rule\": ").append(quote(config.ruleCode() + "@" + config.ruleVersion()))
                .append("},\n");
        if (result != null) {
            json.append("  \"activePublication\": {\"eventId\": ")
                    .append(quote(result.publication().getEventId()))
                    .append(", \"partition\": ").append(result.source().partition())
                    .append(", \"offset\": ").append(result.source().offset()).append("},\n")
                    .append("  \"inactivePublication\": {\"active\": false, \"partition\": ")
                    .append(result.output().partition()).append(", \"offset\": ")
                    .append(result.output().offset()).append("},\n")
                    .append("  \"flinkApplication\": {\"status\": ")
                    .append(quote(result.application().event().getStatus().name()))
                    .append(", \"deploymentId\": ")
                    .append(quote(result.application().event().getDeploymentId()))
                    .append(", \"partition\": ").append(result.application().partition())
                    .append(", \"offset\": ").append(result.application().offset()).append("},\n");
        }
        json.append("  \"error\": ").append(error == null ? "null" : quote(error)).append("\n}");
        Files.createDirectories(config.reportPath().getParent());
        Files.writeString(config.reportPath(), json + "\n", StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    record LocatedPublication(
            byte[] key,
            RecordHeaders headers,
            BoundaryRulePublicationV1 publication,
            int partition,
            long offset) {
    }

    record LocatedApplication(BoundaryRuleApplicationV1 event, int partition, long offset) {
    }

    record Result(
            LocatedPublication source,
            BoundaryRulePublicationV1 publication,
            RecordMetadata output,
            LocatedApplication application) {
    }

    record Config(
            String bootstrapServers,
            String ruleTopic,
            String applicationTopic,
            String marker,
            String tenantId,
            String plantId,
            String lineId,
            String ruleCode,
            String ruleVersion,
            Duration timeout,
            Path reportPath) {

        private static final Pattern SAFE_TOPIC = Pattern.compile("[A-Za-z0-9._-]+");
        private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9._-]{1,128}");

        Config {
            required(bootstrapServers, "bootstrapServers");
            topic(ruleTopic, "ruleTopic");
            topic(applicationTopic, "applicationTopic");
            token(marker, "marker");
            token(tenantId, "tenantId");
            token(plantId, "plantId");
            token(lineId, "lineId");
            token(ruleCode, "ruleCode");
            token(ruleVersion, "ruleVersion");
            if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            if (reportPath == null || !reportPath.isAbsolute()) {
                throw new IllegalArgumentException("reportPath must be absolute");
            }
        }

        static Config fromEnvironment(Map<String, String> environment) {
            return new Config(
                    value(environment, "BPI_KAFKA_BOOTSTRAP_SERVERS", null),
                    value(environment, "BPI_RULE_TOPIC", "bpi.boundary.rule-publication.v1"),
                    value(environment, "BPI_RULE_APPLICATION_TOPIC", "bpi.boundary.rule-application.v1"),
                    value(environment, "BPI_DEACTIVATE_MARKER", null),
                    value(environment, "BPI_DEACTIVATE_TENANT_ID", null),
                    value(environment, "BPI_DEACTIVATE_PLANT_ID", null),
                    value(environment, "BPI_DEACTIVATE_LINE_ID", null),
                    value(environment, "BPI_DEACTIVATE_RULE_CODE", null),
                    value(environment, "BPI_DEACTIVATE_RULE_VERSION", null),
                    Duration.ofSeconds(number(environment, "BPI_DEACTIVATE_TIMEOUT_SECONDS", 180)),
                    Path.of(value(environment, "BPI_DEACTIVATE_REPORT", "/evidence/bpi-rule-deactivation.json")));
        }

        private static long number(Map<String, String> values, String key, long defaultValue) {
            try {
                return Long.parseLong(values.getOrDefault(key, Long.toString(defaultValue)));
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException(key + " must be an integer", error);
            }
        }

        private static String value(Map<String, String> values, String key, String defaultValue) {
            String result = values.getOrDefault(key, defaultValue);
            required(result, key);
            return result.trim();
        }

        private static void topic(String value, String field) {
            required(value, field);
            if (!SAFE_TOPIC.matcher(value).matches()) {
                throw new IllegalArgumentException(field + " contains unsupported characters");
            }
        }

        private static void token(String value, String field) {
            required(value, field);
            if (!SAFE_TOKEN.matcher(value).matches() || value.indexOf('|') >= 0) {
                throw new IllegalArgumentException(field + " contains unsupported characters");
            }
        }

        private static void required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
        }
    }
}
