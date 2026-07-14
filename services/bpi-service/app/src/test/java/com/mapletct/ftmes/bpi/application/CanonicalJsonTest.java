package com.mapletct.ftmes.bpi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalJsonTest {

    private final CanonicalJson canonicalJson = new CanonicalJson(new ObjectMapper());

    @Test
    void producesStableJsonWhenObjectKeysHaveDifferentInsertionOrder() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("version", "1.0.0");
        first.put("definition", Map.of("z", 3, "a", 1));
        first.put("signals", List.of(Map.of("unit", "t/h", "name", "feed.flow")));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("signals", List.of(Map.of("name", "feed.flow", "unit", "t/h")));
        second.put("definition", Map.of("a", 1, "z", 3));
        second.put("version", "1.0.0");

        assertThat(canonicalJson.write(first))
                .isEqualTo(canonicalJson.write(second))
                .isEqualTo("{\"definition\":{\"a\":1,\"z\":3},\"signals\":[{\"name\":\"feed.flow\",\"unit\":\"t/h\"}],\"version\":\"1.0.0\"}");
    }
}
