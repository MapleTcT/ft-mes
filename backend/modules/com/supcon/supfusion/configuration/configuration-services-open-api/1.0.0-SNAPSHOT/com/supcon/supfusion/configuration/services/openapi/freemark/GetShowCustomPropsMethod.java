package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.service.ViewService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class GetShowCustomPropsMethod implements TemplateMethodModelEx {

	@Autowired
	ViewService viewService;

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments != null && arguments.size() >= 3) {
			String modelCode = (String) arguments.get(0);
			String associatedCode = (String) arguments.get(1);
			String viewType = (String) arguments.get(2);
			String propertyLayRec = null;
			if (arguments.size() == 4) {
				propertyLayRec = (String) arguments.get(3);
			}
				return viewService.findShowedCustomProps(modelCode, associatedCode, viewType, propertyLayRec);
		}
		return Collections.EMPTY_LIST;
	}

}
