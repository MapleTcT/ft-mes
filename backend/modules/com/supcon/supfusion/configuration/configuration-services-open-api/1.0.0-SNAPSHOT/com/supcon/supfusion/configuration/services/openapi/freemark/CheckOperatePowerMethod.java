package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckOperatePowerMethod implements TemplateMethodModelEx {
	/***
	 * @param list
	 *            1 operateCode
	 * 
	 ***/
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {

		return true;
	}

}