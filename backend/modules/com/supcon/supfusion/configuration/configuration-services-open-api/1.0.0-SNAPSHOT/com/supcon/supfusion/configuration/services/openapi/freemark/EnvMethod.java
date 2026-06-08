package com.supcon.supfusion.configuration.services.openapi.freemark;

import java.util.List;

import com.supcon.supfusion.configuration.services.utils.PropertyHolder;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

@Component
public class EnvMethod implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		return PropertyHolder.isDev();
	}

}
