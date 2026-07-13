package com.mapletct.ftmes.bpiadapter;

import org.junit.Test;
import org.springframework.http.HttpMethod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BpiRoutePolicyTest {

    private final BpiRoutePolicy policy = new BpiRoutePolicy();

    @Test
    public void allowsOnlyPhaseOneReadsLifecycleAndRuleManagementRoutes() {
        assertTrue(policy.allows(HttpMethod.GET, "/overview"));
        assertTrue(policy.allows(HttpMethod.GET, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4/timeline"));
        assertTrue(policy.allows(HttpMethod.POST, "/candidates/9c392d57-7502-4cd8-bc37-e72961bb08b4/confirm"));
        assertTrue(policy.allows(HttpMethod.POST, "/candidates/9c392d57-7502-4cd8-bc37-e72961bb08b4/reject"));
        assertTrue(policy.allows(HttpMethod.POST, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4/suspend"));
        assertTrue(policy.allows(HttpMethod.POST, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4/resume"));
        assertTrue(policy.allows(HttpMethod.GET, "/topologies"));
        assertTrue(policy.allows(HttpMethod.GET, "/topologies/TOPO-S07-3"));
        assertTrue(policy.allows(HttpMethod.GET, "/rules"));
        assertTrue(policy.allows(HttpMethod.GET, "/rules/RULE-S07-START"));
        assertTrue(policy.allows(HttpMethod.GET, "/rule-simulations/SIM-S07-001"));
        assertTrue(policy.allows(HttpMethod.POST, "/rules/RULE-S07-START/simulate"));
        assertTrue(policy.allows(HttpMethod.POST, "/rules/RULE-S07-START/publish"));
        assertTrue(policy.allows(HttpMethod.POST, "/rules/RULE-S07-START/publication/retry"));
        assertFalse(policy.allows(HttpMethod.POST, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4/force-close"));
        assertFalse(policy.allows(HttpMethod.POST, "/candidates"));
        assertFalse(policy.allows(HttpMethod.POST, "/rules/drafts"));
        assertFalse(policy.allows(HttpMethod.POST, "/topologies/TOPO-S07-3/publish"));
        assertFalse(policy.allows(HttpMethod.DELETE, "/batches/9c392d57-7502-4cd8-bc37-e72961bb08b4"));
        assertFalse(policy.allows(HttpMethod.GET, "/http://attacker.example"));
        assertFalse(policy.allows(HttpMethod.GET, "/../actuator/env"));
    }
}
