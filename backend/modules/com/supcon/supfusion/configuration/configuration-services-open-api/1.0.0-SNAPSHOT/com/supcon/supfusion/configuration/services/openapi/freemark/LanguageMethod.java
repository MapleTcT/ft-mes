package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.Language;
import com.supcon.supfusion.base.services.InternationalService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class LanguageMethod implements TemplateMethodModelEx {

	@Autowired
	private InternationalService internationalService;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		List<Language> list= internationalService.getAllLanguage();
		return list;
	}

}