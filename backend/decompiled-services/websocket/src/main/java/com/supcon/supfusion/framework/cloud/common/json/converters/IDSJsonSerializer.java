/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.JsonGenerator
 *  com.fasterxml.jackson.databind.JsonSerializer
 *  com.fasterxml.jackson.databind.SerializerProvider
 *  org.springframework.util.CollectionUtils
 */
package com.supcon.supfusion.framework.cloud.common.json.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.List;
import org.springframework.util.CollectionUtils;

public class IDSJsonSerializer
extends JsonSerializer<List<Long>> {
    public void serialize(List<Long> longs, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (!CollectionUtils.isEmpty(longs)) {
            jsonGenerator.writeStartArray();
            for (Long l : longs) {
                jsonGenerator.writeString(Long.toString(l));
            }
            jsonGenerator.writeEndArray();
        } else {
            jsonGenerator.writeStartArray();
            jsonGenerator.writeEndArray();
        }
    }
}

