/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.core.JsonParser$Feature
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.DeserializationFeature
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.Module
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.fasterxml.jackson.databind.SerializationFeature
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.framework.cloud.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.supcon.supfusion.framework.cloud.common.exception.ExceptionTool;
import com.supcon.supfusion.framework.cloud.common.time.JavaTimeModule;
import com.supcon.supfusion.framework.cloud.common.util.StringExUtil;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    public static <T> String toJson(T value) {
        try {
            return JsonUtil.getInstance().writeValueAsString(value);
        }
        catch (Exception e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static byte[] toJsonAsBytes(Object object) {
        try {
            return JsonUtil.getInstance().writeValueAsBytes(object);
        }
        catch (JsonProcessingException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T parse(String content, Class<T> valueType) {
        try {
            return (T)JsonUtil.getInstance().readValue(content, valueType);
        }
        catch (Exception e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T parse(String content, TypeReference<?> typeReference) {
        try {
            return (T)JsonUtil.getInstance().readValue(content, typeReference);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T parse(byte[] bytes, Class<T> valueType) {
        try {
            return (T)JsonUtil.getInstance().readValue(bytes, valueType);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T parse(byte[] bytes, TypeReference<?> typeReference) {
        try {
            return (T)JsonUtil.getInstance().readValue(bytes, typeReference);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T parse(InputStream in, Class<T> valueType) {
        try {
            return (T)JsonUtil.getInstance().readValue(in, valueType);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T parse(InputStream in, TypeReference<?> typeReference) {
        try {
            return (T)JsonUtil.getInstance().readValue(in, typeReference);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> List<T> parseArray(String content, Class<T> valueTypeRef) {
        try {
            if (!StringExUtil.startsWithIgnoreCase((String)content, (String)"[")) {
                content = "[" + content + "]";
            }
            List list = (List)JsonUtil.getInstance().readValue(content, new TypeReference<List<T>>(){});
            ArrayList<T> result = new ArrayList<T>();
            for (Map map : list) {
                result.add(JsonUtil.toPojo(map, valueTypeRef));
            }
            return result;
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static Map<String, Object> toMap(String content) {
        try {
            return (Map)JsonUtil.getInstance().readValue(content, Map.class);
        }
        catch (IOException e) {
            log.error(ExceptionTool.getStackTraceAsString(e), (Throwable)e);
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> Map<String, T> toMap(String content, Class<T> valueTypeRef) {
        try {
            Map map = (Map)JsonUtil.getInstance().readValue(content, new TypeReference<Map<String, T>>(){});
            HashMap result = new HashMap(16);
            for (Map.Entry entry : map.entrySet()) {
                result.put(entry.getKey(), JsonUtil.toPojo((Map)entry.getValue(), valueTypeRef));
            }
            return result;
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static <T> T toPojo(Map fromValue, Class<T> toValueType) {
        return (T)JsonUtil.getInstance().convertValue((Object)fromValue, toValueType);
    }

    public static JsonNode readTree(String jsonString) {
        try {
            return JsonUtil.getInstance().readTree(jsonString);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static JsonNode readTree(InputStream in) {
        try {
            return JsonUtil.getInstance().readTree(in);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static JsonNode readTree(byte[] content) {
        try {
            return JsonUtil.getInstance().readTree(content);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static JsonNode readTree(JsonParser jsonParser) {
        try {
            return (JsonNode)JsonUtil.getInstance().readTree(jsonParser);
        }
        catch (IOException e) {
            throw ExceptionTool.unchecked(e);
        }
    }

    public static ObjectMapper getInstance() {
        return JacksonHolder.INSTANCE;
    }

    public static class JacksonObjectMapper
    extends ObjectMapper {
        private static final long serialVersionUID = 4288193147502386170L;
        private static final Locale CHINA = Locale.CHINA;

        public JacksonObjectMapper() {
            super.setLocale(CHINA);
            super.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            super.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
            super.setDateFormat((DateFormat)new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA));
            super.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            super.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
            super.findAndRegisterModules();
            super.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            super.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            super.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            super.getDeserializationConfig().withoutFeatures(new DeserializationFeature[]{DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES});
            super.registerModule((Module)new JavaTimeModule());
            super.findAndRegisterModules();
        }

        public ObjectMapper copy() {
            return super.copy();
        }
    }

    private static class JacksonHolder {
        private static ObjectMapper INSTANCE = new JacksonObjectMapper();

        private JacksonHolder() {
        }
    }
}

