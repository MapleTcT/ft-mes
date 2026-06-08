package com.supcon.supfusion.configuration.services.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * BAP Ec实体自定义序列化类
 * @author zhengjiefeng
 *
 */
public class BAPEcJsonSerializer extends JsonSerializer<AbstractCodeEntity> {
	private Logger logger= LoggerFactory.getLogger(getClass());
	
	private boolean typeFlag=false;
	@Override
	public void serialize(AbstractCodeEntity value, JsonGenerator gen,
			SerializerProvider serializers) throws IOException,
            JsonProcessingException {
		if(!typeFlag){
			gen.writeStartObject();
		}
		gen.writeStringField("code", value.getCode());
		gen.writeStringField("className", value.getClass().getName());
		if(!typeFlag){
			gen.writeEndObject();
		}
		typeFlag=false;
	}
	
	@Override
	public void serializeWithType(AbstractCodeEntity value, JsonGenerator gen,
                                  SerializerProvider serializers, TypeSerializer typeSer)
			throws IOException {
		typeFlag=true;
		typeSer.writeTypePrefixForObject(value, gen);
		serialize(value, gen, serializers);
		typeSer.writeTypeSuffixForObject(value, gen);
	
	}
}
