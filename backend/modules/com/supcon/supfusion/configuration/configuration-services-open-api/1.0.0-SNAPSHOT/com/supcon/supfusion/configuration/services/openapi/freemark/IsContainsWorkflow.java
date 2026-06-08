package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.services.MenuInfoService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class IsContainsWorkflow implements TemplateMethodModelEx {

	@Autowired
	private MenuInfoService menuInfoService;

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if(arguments != null){
			return menuInfoService.isContainsWorkflow((String)arguments.get(0));
		}
		return Collections.EMPTY_LIST;	
	}

}