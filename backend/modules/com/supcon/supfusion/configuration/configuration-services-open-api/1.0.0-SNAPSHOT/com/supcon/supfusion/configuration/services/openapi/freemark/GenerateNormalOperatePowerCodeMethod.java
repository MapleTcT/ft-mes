package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import com.supcon.supfusion.configuration.services.utils.OrchidUtils;
import com.supcon.supfusion.configuration.services.service.ViewService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenerateNormalOperatePowerCodeMethod implements TemplateMethodModelEx {

	@Autowired
	private MenuOperateService menuOperateService;

	private ViewService viewService;
	/**
	 * not workflow operate generate powerCode
	 * 
	 * @param list
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments.size() == 0) {
			return "";
		}
		SecurityContext securityContext = SecurityContextHolder.getContext();
		if (null == securityContext)
			return null;
		OrchidAuthenticationToken authentication = (OrchidAuthenticationToken) securityContext.getAuthentication();
		if (null == authentication)
			return null;
		if(arguments.size() == 1) {
			if(arguments.get(0) == null) {
				return "";
			} else {
				String operateCode = arguments.get(0).toString();
				if (operateCode != null && operateCode.length() > 0) {
					MenuOperate mo = null;
					List<MenuOperate> tempOperates = menuOperateService.getByCode(operateCode, 1000L);
					if (tempOperates != null && tempOperates.size() > 0) {
						mo = tempOperates.iterator().next();
						return "__pc__="
								+ new String(OrchidUtils.encode((mo.getCode() + "|" + (mo.getFlowKey() == null ? "" : mo.getFlowKey())).getBytes()));
					}
				}
			}
		} 
		if (arguments.size() == 2) {
			if(arguments.get(1) == null) {
				return "";
			} else {
				String viewCode = (String)arguments.get(1);
				String operateCode = viewService.getListViewCodeByView(viewCode, 1000L);
				if (operateCode != null && operateCode.length() > 0) {
					return "__pc__="
							+ new String(OrchidUtils.encode((operateCode + "|").getBytes()));
				}
			}
		}
		
		return "";
	}

}