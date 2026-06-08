package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.utils.Inflector;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TableizeMethod implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {

		String entityName = (String) arguments.get(0);
		String prefix = null;
		if (arguments.size() > 0)
			prefix = (String) arguments.get(1);
		return Inflector.getInstance().tableize(prefix, entityName);
	}

}