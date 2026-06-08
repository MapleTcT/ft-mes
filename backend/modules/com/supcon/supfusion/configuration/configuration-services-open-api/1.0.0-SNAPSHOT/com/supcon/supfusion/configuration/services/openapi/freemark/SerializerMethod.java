package com.supcon.supfusion.configuration.services.openapi.freemark;

import flexjson.JSONSerializer;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SerializerMethod implements TemplateMethodModelEx {
	JSONSerializer serializer = new JSONSerializer();

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Object value = arguments.get(0);
		String excludes = (String) arguments.get(1);
		String includes = (String) arguments.get(2);
		Boolean deep = (Boolean) arguments.get(3);
		serializer = serializer.exclude("*.class");
		if (null != includes)
			serializer = serializer.include(includes.split(","));
		if (null != excludes)
			serializer = serializer.exclude(excludes.split(","));
		return null != deep && deep ? serializer.deepSerialize(value) : serializer.serialize(value);
	}

}