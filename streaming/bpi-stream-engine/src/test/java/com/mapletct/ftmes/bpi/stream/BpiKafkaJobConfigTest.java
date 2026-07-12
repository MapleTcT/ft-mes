package com.mapletct.ftmes.bpi.stream;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BpiKafkaJobConfigTest {

    @Test
    void requiredDeploymentIdentityBuildsUniqueConsumerAndTransactionPrefixes() {
        BpiKafkaJobConfig config = BpiKafkaJobConfig.from(Map.of(
                "bootstrap-servers", "kafka-1:9092,kafka-2:9092",
                "deployment-id", "prod-a"));

        assertEquals("ft-mes-bpi-telemetry", config.consumerGroup("telemetry"));
        assertEquals("ft-mes-bpi-prod-a-candidate-", config.transactionalIdPrefix("candidate"));
        assertEquals(Duration.ofSeconds(30), config.checkpointInterval());
        assertEquals(Duration.ofDays(30), config.boundaryStateTtl());
        assertEquals("iot.telemetry.selected.v1", config.telemetryTopic());
    }

    @Test
    void invalidTimingAndTopicReuseFailClosed() {
        Map<String, String> invalidTiming = values();
        invalidTiming.put("checkpoint-timeout-ms", "1000");
        invalidTiming.put("checkpoint-interval-ms", "1000");
        Map<String, String> duplicateTopic = values();
        duplicateTopic.put("candidate-topic", "bpi.data-quality.v1");

        assertThrows(IllegalArgumentException.class, () -> BpiKafkaJobConfig.from(invalidTiming));
        assertThrows(IllegalArgumentException.class, () -> BpiKafkaJobConfig.from(duplicateTopic));
    }

    @Test
    void unsafeDeploymentTokenAndMissingBootstrapFailClosed() {
        Map<String, String> unsafe = values();
        unsafe.put("deployment-id", "prod/a");

        assertThrows(IllegalArgumentException.class, () -> BpiKafkaJobConfig.from(unsafe));
        assertThrows(IllegalArgumentException.class, () -> BpiKafkaJobConfig.from(Map.of(
                "deployment-id", "prod-a")));
    }

    private static Map<String, String> values() {
        Map<String, String> values = new HashMap<>();
        values.put("bootstrap-servers", "kafka:9092");
        values.put("deployment-id", "test-a");
        return values;
    }
}
