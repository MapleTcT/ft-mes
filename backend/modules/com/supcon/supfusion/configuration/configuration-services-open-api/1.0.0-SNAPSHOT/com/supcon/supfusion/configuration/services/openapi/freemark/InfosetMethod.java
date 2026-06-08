package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.service.InfoSetService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InfosetMethod implements TemplateMethodModelEx {

	@Autowired
	private InfoSetService infoSetService;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		
		Long entityId;
		Long userId=-1L;
		Long companyId=-1L;
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)&&null != arguments.get(1)&&null != arguments.get(2)) {
			entityId= Long.valueOf(arguments.get(0).toString());
			userId=Long.valueOf(arguments.get(1).toString());
			companyId=Long.valueOf(arguments.get(2).toString());
		} else {
			return "";
		}
		
		
		Object[] objects;
		try {
			objects = infoSetService.getInfoSets(entityId, userId,companyId);
			return objects;
		} catch (Exception e) {
			
		}

		return null;
	}

}