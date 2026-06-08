package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class I18nMethod implements TemplateMethodModelEx {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String key = String.valueOf(arguments.get(0));
		// arguments.remove(0);
		List newArr = Collections.EMPTY_LIST;
		if (arguments.size() > 1) {
			newArr = new ArrayList(arguments.size() - 1);
			for (int i = 1; i < arguments.size(); i++)
				newArr.add(arguments.get(i));
		}

		return InternationalResource.get(key, newArr.toArray());
	}
}