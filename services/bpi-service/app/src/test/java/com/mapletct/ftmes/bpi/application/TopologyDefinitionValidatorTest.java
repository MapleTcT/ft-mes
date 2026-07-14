package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyDefinitionValidatorTest {

    private final TopologyDefinitionValidator validator = new TopologyDefinitionValidator(new ObjectMapper());

    @Test
    void acceptsACompleteAcyclicTopology() {
        var result = validator.validate(validDefinition());

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void reportsPathBindingAndRequiredSignalFailuresTogether() {
        Map<String, Object> definition = Map.of(
                "localityGroup", "LINE-01",
                "nodes", List.of(
                        Map.of("code", "FEED-TANK", "type", "TANK"),
                        Map.of("code", "FLOW-METER", "type", "METER")),
                "edges", List.of(
                        Map.of("from", "FEED-TANK", "to", "FLOW-METER"),
                        Map.of("from", "FLOW-METER", "to", "FEED-TANK"),
                        Map.of("from", "MISSING", "to", "FLOW-METER")),
                "bindings", List.of(
                        binding("feed.flow", "flow", ""),
                        binding("feed.flow.copy", "flow", "")),
                "requiredSignals", List.of("feed.flow", "tank.level"));

        var result = validator.validate(definition);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .extracting(issue -> issue.code())
                .contains(
                        "PROCESS_PATH_CYCLE",
                        "EDGE_FROM_DANGLING",
                        "SHARED_POINT_UNALLOCATED",
                        "REQUIRED_SIGNAL_UNBOUND");
    }

    @Test
    void requiresProductDevicePropertyUnitAndCalibrationForEveryBinding() {
        Map<String, Object> definition = Map.of(
                "localityGroup", "LINE-01",
                "bindings", List.of(Map.of("signal", "feed.flow")));

        var result = validator.validate(definition);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .extracting(issue -> issue.path())
                .contains(
                        "/bindings/0/productId",
                        "/bindings/0/deviceId",
                        "/bindings/0/propertyId",
                        "/bindings/0/expectedUnit",
                        "/bindings/0/calibrationVersion");
        assertThat(result.warnings())
                .extracting(issue -> issue.code())
                .containsExactlyInAnyOrder("NODES_MISSING", "EDGES_MISSING");
    }

    private static Map<String, Object> validDefinition() {
        return Map.of(
                "localityGroup", "LINE-01",
                "nodes", List.of(
                        Map.of("code", "FEED-TANK", "type", "TANK"),
                        Map.of("code", "FLOW-METER", "type", "METER"),
                        Map.of("code", "RECEIVE-TANK", "type", "TANK")),
                "edges", List.of(
                        Map.of("from", "FEED-TANK", "to", "FLOW-METER"),
                        Map.of("from", "FLOW-METER", "to", "RECEIVE-TANK")),
                "bindings", List.of(
                        binding("feed.flow", "flow", ""),
                        binding("receive.level", "level", "")),
                "requiredSignals", List.of("feed.flow", "receive.level"));
    }

    private static Map<String, Object> binding(String signal, String propertyId, String allocationKey) {
        return Map.of(
                "signal", signal,
                "productId", "PRODUCT-01",
                "deviceId", "DEVICE-01",
                "propertyId", propertyId,
                "expectedUnit", "t/h",
                "calibrationVersion", "CAL-1",
                "allocationKey", allocationKey);
    }
}
