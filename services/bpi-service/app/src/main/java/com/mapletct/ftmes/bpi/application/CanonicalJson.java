package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class CanonicalJson {
    private final ObjectMapper objectMapper;

    public CanonicalJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(canonical(objectMapper.valueToTree(value)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Value cannot be serialized as canonical JSON", exception);
        }
    }

    private JsonNode canonical(JsonNode node) {
        if (node == null || node.isValueNode()) return node;
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(item -> array.add(canonical(item)));
            return array;
        }
        ObjectNode object = objectMapper.createObjectNode();
        List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        iterator.forEachRemaining(fields::add);
        fields.sort(Comparator.comparing(Map.Entry::getKey));
        fields.forEach(entry -> object.set(entry.getKey(), canonical(entry.getValue())));
        return object;
    }
}
