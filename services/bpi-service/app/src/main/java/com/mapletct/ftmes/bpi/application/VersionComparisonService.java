package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.mapletct.ftmes.bpi.domain.VersionChangeView;
import com.mapletct.ftmes.bpi.domain.VersionComparisonView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Component
public class VersionComparisonService {
    private static final int MAX_CHANGES = 500;

    private final ObjectMapper objectMapper;

    public VersionComparisonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VersionComparisonView compare(
            String objectType,
            VersionComparisonView.VersionReference base,
            Object baseContent,
            VersionComparisonView.VersionReference target,
            Object targetContent) {
        List<VersionChangeView> changes = new ArrayList<>();
        Counter counter = new Counter();
        compareNode("", objectMapper.valueToTree(baseContent), objectMapper.valueToTree(targetContent), changes, counter);
        return new VersionComparisonView(
                objectType,
                base,
                target,
                counter.total == 0,
                counter.total,
                counter.total > changes.size(),
                List.copyOf(changes));
    }

    private void compareNode(
            String path,
            JsonNode before,
            JsonNode after,
            List<VersionChangeView> changes,
            Counter counter) {
        if (before.equals(after)) return;
        if (before.isObject() && after.isObject()) {
            TreeSet<String> names = new TreeSet<>();
            before.fieldNames().forEachRemaining(names::add);
            after.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                compareNode(pointer(path, name), before.path(name), after.path(name), changes, counter);
            }
            return;
        }
        if (before.isArray() && after.isArray()) {
            int size = Math.max(before.size(), after.size());
            for (int index = 0; index < size; index++) {
                compareNode(path + "/" + index, before.path(index), after.path(index), changes, counter);
            }
            return;
        }
        counter.total++;
        if (changes.size() >= MAX_CHANGES) return;
        String changeType = before.isMissingNode()
                ? "ADDED"
                : after.isMissingNode() ? "REMOVED" : "CHANGED";
        changes.add(new VersionChangeView(
                path.isEmpty() ? "/" : path,
                changeType,
                value(before),
                value(after)));
    }

    private JsonNode value(JsonNode node) {
        return node instanceof MissingNode ? null : node;
    }

    private String pointer(String path, String segment) {
        return path + "/" + segment.replace("~", "~0").replace("/", "~1");
    }

    private static final class Counter {
        private int total;
    }
}
