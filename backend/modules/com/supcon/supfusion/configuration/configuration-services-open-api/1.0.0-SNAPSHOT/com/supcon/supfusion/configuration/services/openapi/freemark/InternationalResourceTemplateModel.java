package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InternationalResourceTemplateModel implements TemplateMethodModelEx {

	@Autowired
	private InternationalService internationalService;

	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String key = String.valueOf(arguments.get(0));
		String language = String.valueOf(arguments.get(1));

		return internationalService.getI18nValue(key, null, language);
	}
}
