/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonParser
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.databind.DeserializationContext
 *  com.fasterxml.jackson.databind.JsonDeserializer
 *  org.springframework.util.StringUtils
 */
package com.supcon.supfusion.framework.cloud.common.json.converters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

public class IDSJsonDeserializer
extends JsonDeserializer<List<Long>> {
    public List<Long> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ArrayList<Long> result = new ArrayList<Long>();
        if (jsonParser.isExpectedStartArrayToken()) {
            String tmp = jsonParser.nextTextValue();
            while (!StringUtils.isEmpty((Object)tmp)) {
                result.add(Long.parseLong(tmp));
                tmp = jsonParser.nextTextValue();
            }
        }
        return result;
    }
}

