package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.service.ViewService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ViewMethod implements TemplateMethodModelEx {

	@Autowired
	private ViewService viewService;
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String viewCode="";
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			viewCode= arguments.get(0).toString();
		} else {
			return "";
		}
		Object view = viewService.getView(viewCode);
		return view;
	}
}
