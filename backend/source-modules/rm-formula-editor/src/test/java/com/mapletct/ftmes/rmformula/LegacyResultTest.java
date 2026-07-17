package com.mapletct.ftmes.rmformula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapletct.ftmes.rmformula.api.LegacyResult;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyResultTest {
    @Test
    public void serializesNestedBusinessIdentifiersAsStringsWithoutChangingMetrics() throws Exception {
        Map<String, Object> process = new LinkedHashMap<String, Object>();
        process.put("id", 9200000000000011L);
        process.put("processId", 9200000000000011L);
        process.put("version", 2);
        process.put("valid", true);

        Map<String, Object> formula = new LinkedHashMap<String, Object>();
        formula.put("id", 9100000000000011L);
        formula.put("productId", 1781551145996252L);
        formula.put("count", 1L);
        formula.put("processes", Arrays.asList(process));

        LegacyResult<Map<String, Object>> result = LegacyResult.success(formula);
        Map<String, Object> data = result.getData();
        Map<?, ?> normalizedProcess = (Map<?, ?>) ((java.util.List<?>) data.get("processes")).get(0);
        String json = new ObjectMapper().writeValueAsString(result);

        assertEquals("9100000000000011", data.get("id"));
        assertEquals("1781551145996252", data.get("productId"));
        assertEquals(Long.valueOf(1L), data.get("count"));
        assertEquals("9200000000000011", normalizedProcess.get("id"));
        assertEquals("9200000000000011", normalizedProcess.get("processId"));
        assertEquals(Integer.valueOf(2), normalizedProcess.get("version"));
        assertTrue((Boolean) normalizedProcess.get("valid"));
        assertTrue(json.contains("\"id\":\"9100000000000011\""));
        assertTrue(json.contains("\"processId\":\"9200000000000011\""));
        assertTrue(json.contains("\"version\":2"));
        assertTrue(json.contains("\"count\":1"));
    }
}
