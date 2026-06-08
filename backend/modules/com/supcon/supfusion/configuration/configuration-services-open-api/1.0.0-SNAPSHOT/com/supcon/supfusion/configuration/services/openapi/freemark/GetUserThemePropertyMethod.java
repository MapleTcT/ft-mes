package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetUserThemePropertyMethod implements TemplateMethodModelEx {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments != null && !arguments.isEmpty()) {
			Object key = arguments.get(0);
			if (key == null) {
				return null;
			}
//			return themeService.getThemeCode();
		}
		return "preview-blue";
	}
}
