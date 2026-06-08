package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.base.services.UserPermissionService;
import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckUserOperatePowerMethod implements TemplateMethodModelEx {

	@Autowired
	private MenuOperateService menuOperateService;
	@Autowired
	private UserPermissionService userPermissionService;


	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments.get(0) == null) {
			return null;
		}
		String url_pattern = arguments.get(0).toString();
		
	
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (null == securityContext)
			return null;
		OrchidAuthenticationToken authentication = (OrchidAuthenticationToken) securityContext.getAuthentication();
		if(null == authentication) return null;
		Company company = authentication.getCurrentCompany();
		User user=authentication.getCurrentUser();
				
		List<MenuOperate> list=menuOperateService.getMenuOperateByNamespace(url_pattern);
		if(list==null||list.isEmpty()) return false;
		for(MenuOperate mp:list){
			if(null!=userPermissionService.findPermissionByOperateCodeAndUserId(mp, user.getId())){
				return true;
			}
		}
		return false;
	}

}