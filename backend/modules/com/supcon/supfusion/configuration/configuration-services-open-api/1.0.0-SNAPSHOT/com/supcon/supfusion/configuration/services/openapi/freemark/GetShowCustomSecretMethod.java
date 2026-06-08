package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.service.ViewService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GetShowCustomSecretMethod implements TemplateMethodModelEx {
	@Autowired
	private ViewService viewService;
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments != null && arguments.size() >= 3) {
			String modelCode = arguments.get(0).toString();
			String associatedCode = arguments.get(1).toString();
			String viewType = arguments.get(2).toString();
			String propertyLayRec = null;
			if (arguments.size() == 4) {
				propertyLayRec = arguments.get(3).toString();
			}
			return viewService.findCustomPropertyForSecret(modelCode,associatedCode,viewType,propertyLayRec);
		}
		return Collections.EMPTY_LIST;
	}
}
