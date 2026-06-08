package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SystemCodeListMethod implements TemplateMethodModelEx {

	@Autowired
	private SystemCodeService systemCodeService;

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String entityCode = "";
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (null == securityContext)
			return null;
		OrchidAuthenticationToken authentication = (OrchidAuthenticationToken) securityContext.getAuthentication();
		Company company = authentication.getCurrentCompany();
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			entityCode = arguments.get(0).toString();
		} else {
			return Collections.EMPTY_MAP;
		}
		Map<String, String> systemCodeMap = null; //getSystemCodeService().getSystemCodeList(company, entityCode);
		if(arguments!=null && arguments.size()==2){
			systemCodeMap = systemCodeService.getSystemCodeMap(entityCode);
		}else{
			systemCodeMap = systemCodeService.getSystemCodeList(entityCode, false);
		}
		return systemCodeMap;
	}

}