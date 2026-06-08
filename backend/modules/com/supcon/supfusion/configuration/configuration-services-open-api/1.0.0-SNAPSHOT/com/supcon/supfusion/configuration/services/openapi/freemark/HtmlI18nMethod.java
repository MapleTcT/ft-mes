package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.framework.cloud.i18n.context.support.RemoteBundleMessageSource;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HtmlI18nMethod implements TemplateMethodModelEx {


//	@Autowired
	RemoteBundleMessageSource remoteBundleMessageSource;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String key = String.valueOf(arguments.get(0));
 		List newArr = Collections.EMPTY_LIST;
              		if (arguments.size() > 1) {
			newArr = new ArrayList(arguments.size() - 1);
			for (int i = 1; i < arguments.size(); i++)
				newArr.add(arguments.get(i));
		}

		if (null != key) {
			String value = InternationalResource.get(key, newArr.toArray());
			return new SimpleScalar("<span i18n='"+key+"'>"+(value!=null?value:key)+"</span>");
		}
		return null;
	}




}