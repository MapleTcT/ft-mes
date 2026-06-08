package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.service.ViewService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GetPropDisplayNameMethod implements TemplateMethodModelEx {

	@Autowired
	ViewService viewService;
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments != null) {
			String propertyLayRec = arguments.get(0).toString();
			String modelCode = arguments.get(1).toString();
				return viewService.findPropDisplayName(propertyLayRec,modelCode,"ec");
		}
		return Collections.EMPTY_LIST;
	}

}
