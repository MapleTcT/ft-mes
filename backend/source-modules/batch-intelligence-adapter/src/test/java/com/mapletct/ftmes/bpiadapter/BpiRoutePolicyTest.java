package com.mapletct.ftmes.bpiadapter;

import org.junit.Test;
import org.springframework.http.HttpMethod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BpiRoutePolicyTest {

    private final BpiRoutePolicy policy = new BpiRoutePolicy();

    @Test
    public void allowsOnlyPhaseOneReadAndConfirmRoutes() {
        assertTrue(policy.allows(HttpMethod.GET, "/overview"));
        assertTrue(policy.allows(HttpMethod.GET, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4/timeline"));
        assertTrue(policy.allows(HttpMethod.POST, "/candidates/9c392d57-7502-4cd8-bc37-e72961bb08b4/confirm"));
        assertFalse(policy.allows(HttpMethod.POST, "/candidates"));
        assertFalse(policy.allows(HttpMethod.DELETE, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4"));
        assertFalse(policy.allows(HttpMethod.GET, "/http://attacker.example"));
        assertFalse(policy.allows(HttpMethod.GET, "/../actuator/env"));
    }
}
