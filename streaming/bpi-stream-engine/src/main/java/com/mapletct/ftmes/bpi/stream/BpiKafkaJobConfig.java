package com.mapletct.ftmes.bpi.stream;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public record BpiKafkaJobConfig(
        String bootstrapServers,
        String groupPrefix,
        String deploymentId,
        String telemetryTopic,
        String contextTopic,
        String ruleTopic,
        String candidateTopic,
        String dataQualityTopic,
        Duration checkpointInterval,
        Duration checkpointTimeout,
        Duration checkpointMinPause,
        Duration contextWait,
        Duration contextRetention,
        Duration watermarkDelay,
        Duration sourceIdleness,
        Duration boundaryStateTtl,
        Duration transactionTimeout,
        int parallelism) {

    private static final Pattern SAFE_TOKEN = Pattern.compile("[A-Za-z0-9._-]+");

    public BpiKafkaJobConfig {
        require(bootstrapServers, "bootstrapServers");
        token(groupPrefix, "groupPrefix");
        token(deploymentId, "deploymentId");
        topic(telemetryTopic, "telemetryTopic");
        topic(contextTopic, "contextTopic");
        topic(ruleTopic, "ruleTopic");
        topic(candidateTopic, "candidateTopic");
        topic(dataQualityTopic, "dataQualityTopic");
        Set<String> topics = new HashSet<>(List.of(
                telemetryTopic, contextTopic, ruleTopic, candidateTopic, dataQualityTopic));
        if (topics.size() != 5) {
            throw new IllegalArgumentException("BPI Kafka topics must be distinct");
        }
        positive(checkpointInterval, "checkpointInterval");
        positive(checkpointTimeout, "checkpointTimeout");
        nonNegative(checkpointMinPause, "checkpointMinPause");
        positive(contextWait, "contextWait");
        positive(contextRetention, "contextRetention");
        nonNegative(watermarkDelay, "watermarkDelay");
        positive(sourceIdleness, "sourceIdleness");
        positive(boundaryStateTtl, "boundaryStateTtl");
        positive(transactionTimeout, "transactionTimeout");
        if (contextWait.compareTo(contextRetention) > 0) {
            throw new IllegalArgumentException("contextWait cannot exceed contextRetention");
        }
        if (checkpointInterval.compareTo(checkpointTimeout) >= 0) {
            throw new IllegalArgumentException("checkpointTimeout must exceed checkpointInterval");
        }
        if (transactionTimeout.compareTo(checkpointTimeout) <= 0) {
            throw new IllegalArgumentException("transactionTimeout must exceed checkpointTimeout");
        }
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be positive");
        }
    }

    public static BpiKafkaJobConfig fromArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        values.putAll(environment(System.getenv()));
        values.putAll(arguments(args));
        return from(values);
    }

    public static BpiKafkaJobConfig from(Map<String, String> values) {
        return new BpiKafkaJobConfig(
                value(values, "bootstrap-servers", null),
                value(values, "group-prefix", "ft-mes-bpi"),
                value(values, "deployment-id", null),
                value(values, "telemetry-topic", "iot.telemetry.selected.v1"),
                value(values, "context-topic", "mes.production.context.v1"),
                value(values, "rule-topic", "bpi.boundary.rule-publication.v1"),
                value(values, "candidate-topic", "bpi.batch.candidate.v1"),
                value(values, "data-quality-topic", "bpi.data-quality.v1"),
                millis(values, "checkpoint-interval-ms", 30_000),
                millis(values, "checkpoint-timeout-ms", 120_000),
                millis(values, "checkpoint-min-pause-ms", 10_000),
                millis(values, "context-wait-ms", 120_000),
                millis(values, "context-retention-ms", 86_400_000),
                millis(values, "watermark-delay-ms", 30_000),
                millis(values, "source-idleness-ms", 60_000),
                millis(values, "boundary-state-ttl-ms", 2_592_000_000L),
                millis(values, "transaction-timeout-ms", 900_000),
                integer(values, "parallelism", 1));
    }

    public String consumerGroup(String lane) {
        token(lane, "lane");
        return groupPrefix + "-" + lane;
    }

    public String transactionalIdPrefix(String lane) {
        token(lane, "lane");
        return groupPrefix + "-" + deploymentId + "-" + lane + "-";
    }

    private static Map<String, String> environment(Map<String, String> environment) {
        Map<String, String> result = new HashMap<>();
        copy(environment, result, "BPI_KAFKA_BOOTSTRAP_SERVERS", "bootstrap-servers");
        copy(environment, result, "BPI_KAFKA_GROUP_PREFIX", "group-prefix");
        copy(environment, result, "BPI_DEPLOYMENT_ID", "deployment-id");
        copy(environment, result, "BPI_TELEMETRY_TOPIC", "telemetry-topic");
        copy(environment, result, "BPI_CONTEXT_TOPIC", "context-topic");
        copy(environment, result, "BPI_RULE_TOPIC", "rule-topic");
        copy(environment, result, "BPI_CANDIDATE_TOPIC", "candidate-topic");
        copy(environment, result, "BPI_DATA_QUALITY_TOPIC", "data-quality-topic");
        copy(environment, result, "BPI_CHECKPOINT_INTERVAL_MS", "checkpoint-interval-ms");
        copy(environment, result, "BPI_CHECKPOINT_TIMEOUT_MS", "checkpoint-timeout-ms");
        copy(environment, result, "BPI_CHECKPOINT_MIN_PAUSE_MS", "checkpoint-min-pause-ms");
        copy(environment, result, "BPI_CONTEXT_WAIT_MS", "context-wait-ms");
        copy(environment, result, "BPI_CONTEXT_RETENTION_MS", "context-retention-ms");
        copy(environment, result, "BPI_WATERMARK_DELAY_MS", "watermark-delay-ms");
        copy(environment, result, "BPI_SOURCE_IDLENESS_MS", "source-idleness-ms");
        copy(environment, result, "BPI_BOUNDARY_STATE_TTL_MS", "boundary-state-ttl-ms");
        copy(environment, result, "BPI_TRANSACTION_TIMEOUT_MS", "transaction-timeout-ms");
        copy(environment, result, "BPI_PARALLELISM", "parallelism");
        return result;
    }

    private static Map<String, String> arguments(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if (!argument.startsWith("--") || argument.length() == 2) {
                throw new IllegalArgumentException("arguments must use --key=value or --key value syntax");
            }
            String option = argument.substring(2);
            int separator = option.indexOf('=');
            if (separator >= 0) {
                values.put(option.substring(0, separator), option.substring(separator + 1));
                continue;
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("argument has no value: " + argument);
            }
            values.put(option, args[++index]);
        }
        return values;
    }

    private static void copy(
            Map<String, String> source,
            Map<String, String> target,
            String sourceKey,
            String targetKey) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private static String value(Map<String, String> values, String key, String defaultValue) {
        String result = values.getOrDefault(key, defaultValue);
        require(result, key);
        return result.trim();
    }

    private static Duration millis(Map<String, String> values, String key, long defaultValue) {
        try {
            return Duration.ofMillis(Long.parseLong(values.getOrDefault(key, Long.toString(defaultValue))));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be an integer number of milliseconds", error);
        }
    }

    private static int integer(Map<String, String> values, String key, int defaultValue) {
        try {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(key + " must be an integer", error);
        }
    }

    private static void topic(String value, String field) {
        token(value, field);
    }

    private static void token(String value, String field) {
        require(value, field);
        if (!SAFE_TOKEN.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " contains unsupported characters");
        }
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void nonNegative(Duration value, String field) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(field + " cannot be negative");
        }
    }
}
