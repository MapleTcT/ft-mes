package com.mapletct.ftmes.rmformula.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ApiIdentifierNormalizer {
    private ApiIdentifierNormalizer() {
    }

    static Object normalize(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<Object, Object> normalized = new LinkedHashMap<Object, Object>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                Object key = entry.getKey();
                Object item = entry.getValue();
                if (isIdentifierKey(key) && item instanceof Number) {
                    normalized.put(key, String.valueOf(item));
                } else {
                    normalized.put(key, normalize(item));
                }
            }
            return normalized;
        }
        if (value instanceof Collection) {
            List<Object> normalized = new ArrayList<Object>();
            for (Object item : (Collection<?>) value) {
                normalized.add(normalize(item));
            }
            return normalized;
        }
        return value;
    }

    private static boolean isIdentifierKey(Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        String name = (String) key;
        return "id".equals(name) || name.endsWith("Id");
    }
}
