package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.bpi.domain.TopologyValidationIssue;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TopologyDefinitionValidator {
    private static final Set<String> NODE_TYPES = Set.of(
            "STAGE", "LINE", "TANK", "PUMP", "VALVE", "METER", "BOUNDARY", "DEVICE");

    private final ObjectMapper objectMapper;

    public TopologyDefinitionValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidationResult validate(Map<String, Object> definition) {
        JsonNode root = objectMapper.valueToTree(definition);
        List<TopologyValidationIssue> errors = new ArrayList<>();
        List<TopologyValidationIssue> warnings = new ArrayList<>();
        requiredText(root, "localityGroup", "/localityGroup", errors);

        Set<String> nodes = validateNodes(root.path("nodes"), errors, warnings);
        validateEdges(root.path("edges"), nodes, errors, warnings);
        Set<String> signals = validateBindings(root.path("bindings"), errors);
        validateRequiredSignals(root.path("requiredSignals"), signals, errors);
        return new ValidationResult(errors.isEmpty(), List.copyOf(errors), List.copyOf(warnings));
    }

    private Set<String> validateNodes(
            JsonNode nodeArray,
            List<TopologyValidationIssue> errors,
            List<TopologyValidationIssue> warnings) {
        Set<String> nodeCodes = new HashSet<>();
        if (nodeArray.isMissingNode() || nodeArray.isNull()) {
            warnings.add(issue("NODES_MISSING", "/nodes", "WARNING",
                    "No process nodes are configured; path validation is limited."));
            return nodeCodes;
        }
        if (!nodeArray.isArray()) {
            errors.add(issue("NODES_NOT_ARRAY", "/nodes", "ERROR", "nodes must be an array."));
            return nodeCodes;
        }
        for (int index = 0; index < nodeArray.size(); index++) {
            JsonNode node = nodeArray.get(index);
            String path = "/nodes/" + index;
            String code = requiredText(node, "code", path + "/code", errors);
            String type = requiredText(node, "type", path + "/type", errors).toUpperCase();
            if (!code.isBlank() && !nodeCodes.add(code)) {
                errors.add(issue("NODE_CODE_DUPLICATE", path + "/code", "ERROR",
                        "Node code must be unique: " + code));
            }
            if (!type.isBlank() && !NODE_TYPES.contains(type)) {
                errors.add(issue("NODE_TYPE_UNSUPPORTED", path + "/type", "ERROR",
                        "Unsupported node type: " + type));
            }
        }
        return nodeCodes;
    }

    private void validateEdges(
            JsonNode edgeArray,
            Set<String> nodes,
            List<TopologyValidationIssue> errors,
            List<TopologyValidationIssue> warnings) {
        if (edgeArray.isMissingNode() || edgeArray.isNull()) {
            warnings.add(issue("EDGES_MISSING", "/edges", "WARNING",
                    "No directed process path is configured; lineage cycle checks are limited."));
            return;
        }
        if (!edgeArray.isArray()) {
            errors.add(issue("EDGES_NOT_ARRAY", "/edges", "ERROR", "edges must be an array."));
            return;
        }
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        Set<String> edgeKeys = new HashSet<>();
        for (int index = 0; index < edgeArray.size(); index++) {
            JsonNode edge = edgeArray.get(index);
            String path = "/edges/" + index;
            String from = requiredText(edge, "from", path + "/from", errors);
            String to = requiredText(edge, "to", path + "/to", errors);
            if (!from.isBlank() && !nodes.contains(from)) {
                errors.add(issue("EDGE_FROM_DANGLING", path + "/from", "ERROR",
                        "Edge source does not reference a configured node: " + from));
            }
            if (!to.isBlank() && !nodes.contains(to)) {
                errors.add(issue("EDGE_TO_DANGLING", path + "/to", "ERROR",
                        "Edge target does not reference a configured node: " + to));
            }
            if (!from.isBlank() && from.equals(to)) {
                errors.add(issue("EDGE_SELF_LOOP", path, "ERROR", "A process edge cannot target itself."));
            }
            String edgeKey = from + "->" + to;
            if (!from.isBlank() && !to.isBlank() && !edgeKeys.add(edgeKey)) {
                errors.add(issue("EDGE_DUPLICATE", path, "ERROR", "Duplicate process edge: " + edgeKey));
            }
            if (!from.isBlank() && !to.isBlank()) {
                graph.computeIfAbsent(from, ignored -> new HashSet<>()).add(to);
            }
        }
        if (containsCycle(graph)) {
            errors.add(issue("PROCESS_PATH_CYCLE", "/edges", "ERROR",
                    "Directed process paths contain a cycle that can make batch lineage ambiguous."));
        }
    }

    private Set<String> validateBindings(JsonNode bindingArray, List<TopologyValidationIssue> errors) {
        Set<String> signals = new HashSet<>();
        if (!bindingArray.isArray() || bindingArray.isEmpty()) {
            errors.add(issue("BINDINGS_REQUIRED", "/bindings", "ERROR",
                    "At least one JetLinks point binding is required."));
            return signals;
        }
        Map<String, String> sourceOwners = new HashMap<>();
        for (int index = 0; index < bindingArray.size(); index++) {
            JsonNode binding = bindingArray.get(index);
            String path = "/bindings/" + index;
            String signal = requiredText(binding, "signal", path + "/signal", errors);
            String productId = requiredText(binding, "productId", path + "/productId", errors);
            String deviceId = requiredText(binding, "deviceId", path + "/deviceId", errors);
            String propertyId = requiredText(binding, "propertyId", path + "/propertyId", errors);
            String unit = text(binding, "expectedUnit");
            if (unit.isBlank()) unit = text(binding, "unit");
            if (unit.isBlank()) {
                errors.add(issue("BINDING_UNIT_REQUIRED", path + "/expectedUnit", "ERROR",
                        "A binding must declare expectedUnit or unit."));
            }
            requiredText(binding, "calibrationVersion", path + "/calibrationVersion", errors);
            if (!signal.isBlank() && !signals.add(signal)) {
                errors.add(issue("BINDING_SIGNAL_DUPLICATE", path + "/signal", "ERROR",
                        "Semantic signal must be unique: " + signal));
            }
            if (!productId.isBlank() && !deviceId.isBlank() && !propertyId.isBlank()) {
                String source = productId + "/" + deviceId + "/" + propertyId;
                String previous = sourceOwners.putIfAbsent(source, signal);
                if (previous != null && text(binding, "allocationKey").isBlank()) {
                    errors.add(issue("SHARED_POINT_UNALLOCATED", path, "ERROR",
                            "Shared point " + source + " requires allocationKey."));
                }
            }
        }
        return signals;
    }

    private void validateRequiredSignals(
            JsonNode requiredSignals,
            Set<String> signals,
            List<TopologyValidationIssue> errors) {
        if (requiredSignals.isMissingNode() || requiredSignals.isNull()) return;
        if (!requiredSignals.isArray()) {
            errors.add(issue("REQUIRED_SIGNALS_NOT_ARRAY", "/requiredSignals", "ERROR",
                    "requiredSignals must be an array."));
            return;
        }
        for (int index = 0; index < requiredSignals.size(); index++) {
            String signal = requiredSignals.get(index).asText("");
            if (signal.isBlank() || !signals.contains(signal)) {
                errors.add(issue("REQUIRED_SIGNAL_UNBOUND", "/requiredSignals/" + index, "ERROR",
                        "Required signal is not bound: " + signal));
            }
        }
    }

    private boolean containsCycle(Map<String, Set<String>> graph) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String node : graph.keySet()) {
            if (visit(node, graph, visiting, visited)) return true;
        }
        return false;
    }

    private boolean visit(
            String node,
            Map<String, Set<String>> graph,
            Set<String> visiting,
            Set<String> visited) {
        if (visited.contains(node)) return false;
        if (!visiting.add(node)) return true;
        for (String target : graph.getOrDefault(node, Set.of())) {
            if (visit(target, graph, visiting, visited)) return true;
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private String requiredText(
            JsonNode node,
            String field,
            String path,
            List<TopologyValidationIssue> errors) {
        String value = text(node, field);
        if (value.isBlank()) {
            errors.add(issue("FIELD_REQUIRED", path, "ERROR", field + " is required."));
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText().trim() : "";
    }

    private TopologyValidationIssue issue(String code, String path, String severity, String message) {
        return new TopologyValidationIssue(code, path, severity, message);
    }

    public record ValidationResult(
            boolean valid,
            List<TopologyValidationIssue> errors,
            List<TopologyValidationIssue> warnings) {
    }
}
