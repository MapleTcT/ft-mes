package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class DateTimeMethod implements TemplateMethodModelEx {
	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Date date = new Date();

		String pattern;
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			pattern = arguments.get(0).toString();
		} else {
			pattern = DEFAULT_DATE_FORMAT;
		}
		try {
			return new SimpleDateFormat(pattern).format(date);
		} catch (RuntimeException e) {
			return new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(date);
		}
	}

}