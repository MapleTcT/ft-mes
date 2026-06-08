package com.supcon.supfusion.configuration.services.openapi.freemark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

@Component
public class GetConfigPropertyMethod implements TemplateMethodModelEx {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments != null && !arguments.isEmpty()) {
			Object key = arguments.get(0);
			if (key == null) {
				return null;
			}
			String propValue = null;
			propValue = map.get(key.toString());
//			propValue = configHolderService.getProperty(key.toString());
			return propValue;
		}
		return null;
	}

	private static Map<String, String> map = new HashMap();
	static {
		map.put("platform/bap/basic/bap.theme", "default");
		map.put("orchid.env", "false");
	}

}
