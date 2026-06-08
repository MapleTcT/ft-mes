package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.services.CustomPropertyService;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CustomPropertyMethod implements TemplateMethodModelEx {

	@Autowired
	CustomPropertyService customPropertyService;
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments != null && arguments.size() > 2) {
			String moduleCode = (String) arguments.get(0);
			String modelCode = (String) arguments.get(1);
			String businessModelCode = (String) arguments.get(2);
			String businessVals = null;
			if (arguments.size() > 3) {
				businessVals = (String) arguments.get(3);
			}
			try {
				if (arguments.size() > 3 && businessVals != null && businessVals.length() > 0) {
					return customPropertyService.findCPByBusinessValue(modelCode, businessModelCode, (Object[]) businessVals.split(","));
				} else {
					return customPropertyService.findCPByBusinessValue(modelCode, businessModelCode);
				}
			} catch (Throwable e) {
				throw new EcException(e);
			}
		}
		return Collections.EMPTY_LIST;
	}

}
