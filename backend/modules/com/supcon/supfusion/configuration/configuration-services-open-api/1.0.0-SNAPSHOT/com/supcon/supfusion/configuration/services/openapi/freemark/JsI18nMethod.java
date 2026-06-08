package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;


@Component
public class JsI18nMethod implements TemplateMethodModelEx {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Object key = arguments.get(0);
		List newArr = Collections.EMPTY_LIST;
		if (arguments.size() > 1) {
			newArr = new ArrayList(arguments.size() - 1);
			for (int i = 1; i < arguments.size(); i++)
				newArr.add(arguments.get(i));
		}

		if (null != key) {
			String value = InternationalResource.get(String.valueOf(key));
			StringBuilder buf = new StringBuilder();
			StringTokenizer token = new StringTokenizer(value, "'\"",true);
			while(token.hasMoreTokens()) {
				String s = token.nextToken();
				if("'".equals(s)) buf.append("\\'");
				else if("\"".equals(s)) buf.append("\\\"");
				else buf.append(s);
			}
			return new SimpleScalar(buf.toString());
		}
		return null;
	}

}