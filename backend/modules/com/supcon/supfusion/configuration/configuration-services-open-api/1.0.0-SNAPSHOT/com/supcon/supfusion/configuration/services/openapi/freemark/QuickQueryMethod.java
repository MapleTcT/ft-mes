package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.service.ModelService;
import flexjson.JSONDeserializer;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class QuickQueryMethod implements TemplateMethodModelEx {

	@Autowired
	private ModelService modelservice;

	private JSONDeserializer<Map> deserializer = new JSONDeserializer();

	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String propertyCode;
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			propertyCode= arguments.get(0).toString(); 
		} else {
			return "";
		}
		Property property = modelservice.findPropertyByCode(propertyCode);
		if(property!=null&&property.getFillcontent()!=null)
		{
			String fillContent = property.getFillcontent();
			Map<String,Object> fillContentMap = (fillContent !=null && fillContent.startsWith("{")) ? deserializer.deserialize(fillContent) : Collections.EMPTY_MAP;
			Object fillContent2 = fillContentMap.get("fillContent");
			if(fillContent2 instanceof String){
				property.setFillcontent((String) fillContent2);
			}
		}
		return property;
	}
}
