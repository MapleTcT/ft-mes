package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleApplicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessStatusV1;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRuleRuntimeReadinessV1;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Read-only Kafka evidence for one browser-managed rule lifecycle. */
public final class BpiRuleLifecycleEvidence {

    private BpiRuleLifecycleEvidence() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.fromEnvironment(System.getenv());
        try {
            Result result = execute(config);
            writeReport(config, result, "PASS", null);
            System.out.println("BPI rule lifecycle evidence PASS marker=" + config.marker()
                    + " activeOffset=" + result.activePublication().offset()
                    + " retirementOffset=" + result.retirementPublication().offset()
                    + " report=" + config.reportPath());
        } catch (Exception error) {
            writeReport(config, null, "FAIL", error.getMessage());
            throw error;
        }
    }

    static Result execute(Config config) throws Exception {
        List<LocatedPublication> publications;
        List<LocatedApplication> applications;
        List<LocatedReadiness> readiness;
        try (KafkaConsumer<byte[], byte[]> consumer = consumer(config, "publication")) {
            publications = scanPublications(consumer, config);
        }
        try (KafkaConsumer<byte[], byte[]> consumer = consumer(config, "application")) {
            applications = scanApplications(consumer, config);
        }
        try (KafkaConsumer<byte[], byte[]> consumer = consumer(config, "readiness")) {
            readiness = scanReadiness(consumer, config);
        }
        return verify(config, publications, applications, readiness);
    }

    static Result verify(
            Config config,
            List<LocatedPublication> publications,
            List<LocatedApplication> applications,
            List<LocatedReadiness> readiness) {
        List<LocatedPublication> active = publications.stream()
                .filter(value -> value.event().getActive())
                .toList();
        List<LocatedPublication> retired = publications.stream()
                .filter(value -> !value.event().getActive())
                .toList();
        require(active.size() == 1,
                "expected exactly one active publication, found " + active.size());
        require(retired.size() == 1,
                "expected exactly one retirement publication, found " + retired.size());

        LocatedPublication activation = active.get(0);
        LocatedPublication retirement = retired.get(0);
        require(!activation.event().getEventId().equals(retirement.event().getEventId()),
                "activation and retirement reused the same event id");
        require(activation.event().getChecksum().equals(retirement.event().getChecksum()),
                "retirement changed the immutable rule checksum");
        require(retirement.event().getPublishedAtMs() > activation.event().getPublishedAtMs(),
                "retirement publication is not newer than activation");
        require("ACTIVATE".equals(action(activation.event())),
                "active payload does not declare lifecycle_action=ACTIVATE");
        require("RETIRE".equals(action(retirement.event())),
                "inactive payload does not declare lifecycle_action=RETIRE");
        require("ACTIVATE".equals(activation.brokerLifecycleAction()),
                "active Kafka header does not declare lifecycle_action=ACTIVATE");
        require("RETIRE".equals(retirement.brokerLifecycleAction()),
                "retirement Kafka header does not declare lifecycle_action=RETIRE");

        LocatedApplication activeApplication = exactlyOneApplication(
                applications, activation.event().getEventId(), "activation");
        LocatedApplication retirementApplication = exactlyOneApplication(
                applications, retirement.event().getEventId(), "retirement");
        ReadinessSelection activeReadiness = latestReadiness(
                readiness,
                activation.event().getEventId(),
                BoundaryRuleRuntimeReadinessStatusV1.READY,
                "activation READY");
        ReadinessSelection retirementReadiness = latestReadiness(
                readiness,
                retirement.event().getEventId(),
                BoundaryRuleRuntimeReadinessStatusV1.INACTIVE,
                "retirement INACTIVE");
        require(activeApplication.event().getDeploymentId()
                        .equals(retirementApplication.event().getDeploymentId()),
                "activation and retirement were applied by different deployments");
        require(retirementApplication.event().getDeploymentId()
                        .equals(retirementReadiness.value().event().getDeploymentId()),
                "retirement application and readiness deployments differ");
        return new Result(
                activation,
                retirement,
                activeApplication,
                retirementApplication,
                activeReadiness.value(),
                retirementReadiness.value(),
                activeReadiness.count(),
                retirementReadiness.count(),
                activeReadiness.distinctEventIdCount(),
                retirementReadiness.distinctEventIdCount());
    }

    private static LocatedApplication exactlyOneApplication(
            List<LocatedApplication> applications,
            String publicationEventId,
            String label) {
        List<LocatedApplication> matches = applications.stream()
                .filter(value -> value.event().getPublicationEventId().equals(publicationEventId))
                .filter(value -> value.event().getStatus() == BoundaryRuleApplicationStatusV1.APPLIED)
                .toList();
        require(matches.size() == 1,
                "expected exactly one APPLIED receipt for " + label + ", found " + matches.size());
        return matches.get(0);
    }

    private static ReadinessSelection latestReadiness(
            List<LocatedReadiness> readiness,
            String publicationEventId,
            BoundaryRuleRuntimeReadinessStatusV1 status,
            String label) {
        List<LocatedReadiness> matches = readiness.stream()
                .filter(value -> value.event().getPublicationEventId().equals(publicationEventId))
                .filter(value -> value.event().getStatus() == status)
                .toList();
        require(!matches.isEmpty(), "expected at least one " + label + " receipt");
        int partition = matches.get(0).partition();
        require(matches.stream().allMatch(value -> value.partition() == partition),
                label + " receipts were not keyed to one Kafka partition");
        LocatedReadiness latest = matches.stream()
                .max(java.util.Comparator.comparingLong(LocatedReadiness::offset))
                .orElseThrow();
        int distinctEventIdCount = (int) matches.stream()
                .map(value -> value.event().getEventId())
                .distinct()
                .count();
        require(distinctEventIdCount == matches.size(),
                label + " contains duplicate event ids: records=" + matches.size()
                        + ", distinct=" + distinctEventIdCount);
        return new ReadinessSelection(latest, matches.size(), distinctEventIdCount);
    }

    private static List<LocatedPublication> scanPublications(
            KafkaConsumer<byte[], byte[]> consumer,
            Config config) throws InvalidProtocolBufferException {
        TopicScan scan = beginScan(consumer, config.ruleTopic(), config.timeout());
        List<LocatedPublication> result = new ArrayList<>();
        while (Instant.now().isBefore(scan.deadline()) && !atEnd(consumer, scan.endOffsets())) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (record.value() == null) continue;
                BoundaryRulePublicationV1 event = BoundaryRulePublicationV1.parseFrom(record.value());
                if (matches(event, config)) {
                    result.add(new LocatedPublication(
                            event,
                            record.partition(),
                            record.offset(),
                            record.timestamp(),
                            header(record, "lifecycle_action")));
                }
            }
        }
        require(atEnd(consumer, scan.endOffsets()), "publication topic scan timed out");
        return result;
    }

    private static List<LocatedApplication> scanApplications(
            KafkaConsumer<byte[], byte[]> consumer,
            Config config) throws InvalidProtocolBufferException {
        TopicScan scan = beginScan(consumer, config.applicationTopic(), config.timeout());
        List<LocatedApplication> result = new ArrayList<>();
        while (Instant.now().isBefore(scan.deadline()) && !atEnd(consumer, scan.endOffsets())) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (record.value() == null) continue;
                BoundaryRuleApplicationV1 event = BoundaryRuleApplicationV1.parseFrom(record.value());
                if (matches(event, config)) {
                    result.add(new LocatedApplication(
                            event, record.partition(), record.offset(), record.timestamp()));
                }
            }
        }
        require(atEnd(consumer, scan.endOffsets()), "application topic scan timed out");
        return result;
    }

    private static List<LocatedReadiness> scanReadiness(
            KafkaConsumer<byte[], byte[]> consumer,
            Config config) throws InvalidProtocolBufferException {
        TopicScan scan = beginScan(consumer, config.readinessTopic(), config.timeout());
        List<LocatedReadiness> result = new ArrayList<>();
        while (Instant.now().isBefore(scan.deadline()) && !atEnd(consumer, scan.endOffsets())) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<byte[], byte[]> record : records) {
                if (record.value() == null) continue;
                BoundaryRuleRuntimeReadinessV1 event = BoundaryRuleRuntimeReadinessV1.parseFrom(record.value());
                if (matches(event, config)) {
                    result.add(new LocatedReadiness(
                            event, record.partition(), record.offset(), record.timestamp()));
                }
            }
        }
        require(atEnd(consumer, scan.endOffsets()), "readiness topic scan timed out");
        return result;
    }

    private static TopicScan beginScan(
            KafkaConsumer<byte[], byte[]> consumer,
            String topic,
            Duration timeout) {
        List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(info -> new TopicPartition(info.topic(), info.partition()))
                .toList();
        require(!partitions.isEmpty(), topic + " has no partitions");
        consumer.assign(partitions);
        consumer.seekToBeginning(partitions);
        return new TopicScan(consumer.endOffsets(partitions), Instant.now().plus(timeout));
    }

    private static boolean atEnd(
            KafkaConsumer<byte[], byte[]> consumer,
            Map<TopicPartition, Long> endOffsets) {
        for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
            if (consumer.position(entry.getKey()) < entry.getValue()) return false;
        }
        return true;
    }

    static boolean matches(BoundaryRulePublicationV1 event, Config config) {
        return scope(event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getRuleCode(), event.getRuleVersion(), config);
    }

    private static boolean matches(BoundaryRuleApplicationV1 event, Config config) {
        return scope(event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getRuleCode(), event.getRuleVersion(), config);
    }

    private static boolean matches(BoundaryRuleRuntimeReadinessV1 event, Config config) {
        return scope(event.getTenantId(), event.getPlantId(), event.getLineId(),
                event.getRuleCode(), event.getRuleVersion(), config);
    }

    private static boolean scope(
            String tenantId,
            String plantId,
            String lineId,
            String ruleCode,
            String ruleVersion,
            Config config) {
        return tenantId.equals(config.tenantId())
                && plantId.equals(config.plantId())
                && lineId.equals(config.lineId())
                && ruleCode.equals(config.ruleCode())
                && ruleVersion.equals(config.ruleVersion());
    }

    private static String action(BoundaryRulePublicationV1 event) {
        return event.getHeadersOrDefault("lifecycle_action", "");
    }

    private static String header(ConsumerRecord<byte[], byte[]> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static KafkaConsumer<byte[], byte[]> consumer(Config config, String purpose) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,
                "ft-mes-bpi-lifecycle-evidence-" + purpose + "-" + config.marker());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        return new KafkaConsumer<>(properties);
    }

    static void writeReport(Config config, Result result, String status, String error) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"schemaVersion\": 1,\n")
                .append("  \"generatedAt\": ").append(quote(Instant.now().toString())).append(",\n")
                .append("  \"status\": ").append(quote(status)).append(",\n")
                .append("  \"marker\": ").append(quote(config.marker())).append(",\n")
                .append("  \"scope\": {\"tenantId\": ").append(quote(config.tenantId()))
                .append(", \"plantId\": ").append(quote(config.plantId()))
                .append(", \"lineId\": ").append(quote(config.lineId()))
                .append(", \"rule\": ").append(quote(config.ruleCode() + "@" + config.ruleVersion()))
                .append("},\n");
        if (result != null) {
            appendPublication(json, "activationPublication", result.activePublication(), true);
            appendPublication(json, "retirementPublication", result.retirementPublication(), false);
            appendApplication(json, "activationApplication", result.activeApplication());
            appendApplication(json, "retirementApplication", result.retirementApplication());
            appendReadiness(json, "activationReadiness", result.activeReadiness(),
                    result.activeReadinessCount(), result.activeReadinessDistinctEventIdCount());
            appendReadiness(json, "retirementReadiness", result.retirementReadiness(),
                    result.retirementReadinessCount(), result.retirementReadinessDistinctEventIdCount());
            json.append("  \"summary\": {\"activationPublished\": true, \"retirementPublished\": true, ")
                    .append("\"flinkAppliedBoth\": true, \"runtimeReadyThenInactive\": true},\n");
        }
        json.append("  \"error\": ").append(error == null ? "null" : quote(error)).append("\n}\n");
        Files.createDirectories(config.reportPath().getParent());
        Files.writeString(config.reportPath(), json.toString(), StandardCharsets.UTF_8);
    }

    private static void appendPublication(
            StringBuilder json,
            String name,
            LocatedPublication value,
            boolean active) {
        json.append("  ").append(quote(name)).append(": {\"eventId\": ")
                .append(quote(value.event().getEventId()))
                .append(", \"active\": ").append(active)
                .append(", \"lifecycleAction\": ").append(quote(action(value.event())))
                .append(", \"brokerLifecycleAction\": ").append(quote(value.brokerLifecycleAction()))
                .append(", \"partition\": ").append(value.partition())
                .append(", \"offset\": ").append(value.offset())
                .append(", \"kafkaTimestamp\": ").append(value.kafkaTimestamp())
                .append(", \"publishedAtMs\": ").append(value.event().getPublishedAtMs())
                .append("},\n");
    }

    private static void appendApplication(
            StringBuilder json,
            String name,
            LocatedApplication value) {
        json.append("  ").append(quote(name)).append(": {\"eventId\": ")
                .append(quote(value.event().getEventId()))
                .append(", \"publicationEventId\": ").append(quote(value.event().getPublicationEventId()))
                .append(", \"status\": ").append(quote(value.event().getStatus().name()))
                .append(", \"deploymentId\": ").append(quote(value.event().getDeploymentId()))
                .append(", \"partition\": ").append(value.partition())
                .append(", \"offset\": ").append(value.offset())
                .append(", \"kafkaTimestamp\": ").append(value.kafkaTimestamp())
                .append("},\n");
    }

    private static void appendReadiness(
            StringBuilder json,
            String name,
            LocatedReadiness value,
            int matchingStatusCount,
            int distinctEventIdCount) {
        json.append("  ").append(quote(name)).append(": {\"eventId\": ")
                .append(quote(value.event().getEventId()))
                .append(", \"publicationEventId\": ").append(quote(value.event().getPublicationEventId()))
                .append(", \"status\": ").append(quote(value.event().getStatus().name()))
                .append(", \"deploymentId\": ").append(quote(value.event().getDeploymentId()))
                .append(", \"partition\": ").append(value.partition())
                .append(", \"offset\": ").append(value.offset())
                .append(", \"kafkaTimestamp\": ").append(value.kafkaTimestamp())
                .append(", \"observedAtMs\": ").append(value.event().getObservedAtMs())
                .append(", \"pointCatalogEventId\": ")
                .append(quote(value.event().getPointCatalogEventId()))
                .append(", \"pointCatalogSourceRevision\": ")
                .append(quote(value.event().getPointCatalogSourceRevision()))
                .append(", \"matchingStatusCount\": ").append(matchingStatusCount)
                .append(", \"distinctEventIdCount\": ").append(distinctEventIdCount)
                .append("},\n");
    }

    private static String quote(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    record LocatedPublication(
            BoundaryRulePublicationV1 event,
            int partition,
            long offset,
            long kafkaTimestamp,
            String brokerLifecycleAction) {
    }

    record LocatedApplication(
            BoundaryRuleApplicationV1 event,
            int partition,
            long offset,
            long kafkaTimestamp) {
    }

    record LocatedReadiness(
            BoundaryRuleRuntimeReadinessV1 event,
            int partition,
            long offset,
            long kafkaTimestamp) {
    }

    record Result(
            LocatedPublication activePublication,
            LocatedPublication retirementPublication,
            LocatedApplication activeApplication,
            LocatedApplication retirementApplication,
            LocatedReadiness activeReadiness,
            LocatedReadiness retirementReadiness,
            int activeReadinessCount,
            int retirementReadinessCount,
            int activeReadinessDistinctEventIdCount,
            int retirementReadinessDistinctEventIdCount) {
    }

    private record ReadinessSelection(
            LocatedReadiness value,
            int count,
            int distinctEventIdCount) {
    }

    private record TopicScan(Map<TopicPartition, Long> endOffsets, Instant deadline) {
    }

    record Config(
            String bootstrapServers,
            String ruleTopic,
            String applicationTopic,
            String readinessTopic,
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
            topic(readinessTopic, "readinessTopic");
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
                    value(environment, "BPI_RULE_RUNTIME_READINESS_TOPIC",
                            "bpi.boundary.rule-runtime-readiness.v1"),
                    value(environment, "BPI_LIFECYCLE_EVIDENCE_MARKER", null),
                    value(environment, "BPI_LIFECYCLE_EVIDENCE_TENANT_ID", null),
                    value(environment, "BPI_LIFECYCLE_EVIDENCE_PLANT_ID", null),
                    value(environment, "BPI_LIFECYCLE_EVIDENCE_LINE_ID", null),
                    value(environment, "BPI_LIFECYCLE_EVIDENCE_RULE_CODE", null),
                    value(environment, "BPI_LIFECYCLE_EVIDENCE_RULE_VERSION", null),
                    Duration.ofSeconds(number(environment, "BPI_LIFECYCLE_EVIDENCE_TIMEOUT_SECONDS", 60)),
                    Path.of(value(environment, "BPI_LIFECYCLE_EVIDENCE_REPORT",
                            "/evidence/bpi-rule-lifecycle-evidence.json")));
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
