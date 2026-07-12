package com.mapletct.ftmes.bpi.rules;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundaryTimingPolicyTest {

    @Test
    void timingSemanticsAreExplicitAndBounded() {
        BoundaryTimingPolicy policy = new BoundaryTimingPolicy(
                Duration.ofSeconds(30), Duration.ofSeconds(5), Duration.ofMinutes(2));

        assertEquals(Duration.ofSeconds(30), policy.allowedLateness());
        assertEquals(Duration.ofSeconds(5), policy.watermarkDelay());
        assertEquals(Duration.ofMinutes(2), policy.evaluationTimeout());
        assertThrows(
                IllegalArgumentException.class,
                () -> new BoundaryTimingPolicy(
                        Duration.ofMinutes(3), Duration.ZERO, Duration.ofMinutes(2)));
    }
}
