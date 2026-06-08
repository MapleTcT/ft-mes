package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemCodeMethod implements TemplateMethodModelEx {

	@Autowired
	private SystemCodeService systemCodeService;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		
		String entityCode="";
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			entityCode= arguments.get(0).toString();
		} else {
			return "";
		}
		SystemEntity systemEntity = systemCodeService.getSystemEntityByCode(entityCode);

		return systemEntity;
	}

}