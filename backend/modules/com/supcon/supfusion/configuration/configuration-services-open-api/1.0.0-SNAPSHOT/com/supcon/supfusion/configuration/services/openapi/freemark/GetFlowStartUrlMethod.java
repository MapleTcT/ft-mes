package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.services.DataPermissionService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import com.supcon.supfusion.configuration.services.utils.OrchidUtils;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GetFlowStartUrlMethod implements TemplateMethodModelEx {

	@Autowired
	private MenuOperateService menuOperateService;
	@Autowired
	private DataPermissionService dataPermissionService;

	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Map<String, String> map = new HashMap<>();
		if (arguments.size() == 1) {
			SecurityContext securityContext = SecurityContextHolder.getContext();
			if (null == securityContext)
				return null;
			OrchidAuthenticationToken authentication = (OrchidAuthenticationToken) securityContext.getAuthentication();
			if (null == authentication)
				return null;
			String entityCode = arguments.get(0).toString();
			Long userId = UserContext.getUserContext().getUserId();
			Company company = authentication.getCurrentCompany();
			Set<Map<String, Object>> startList = dataPermissionService.getFlowStart(entityCode, userId);
			if(null != startList){
				for (Map<String, Object> dp : startList) {
					String flowKey = dp.get("FLOWKEY").toString();
					String activeCode = dp.get("ACTIVITYCODE").toString();
					String name = dp.get("NAME").toString();
					List<MenuOperate> menuOperates = menuOperateService.getOperateByCodeAndFlowKey(activeCode, flowKey, company);
					if(null != menuOperates && !menuOperates.isEmpty()){
						for (MenuOperate mo : menuOperates) {
							String url = mo.getUrl();
							if (url == null || mo.getIsHidden()) {
								continue;
							}
							if (null != url && null != mo.getDeploymentId() && MenuOperateType.ACTIVEOPERATE.equals(mo.getMenuOperateType())) {
								if (url.indexOf("?") != -1) {
									url += "&deploymentId=" + mo.getDeploymentId();
								} else {
									url += "?deploymentId=" + mo.getDeploymentId();
								}
								url += "&entityCode=" + entityCode;
							}
							// 添加权限控制
							url += (url.indexOf("?") == -1 ? "?" : "&") + "__pc__="
									+ new String(OrchidUtils.encode((mo.getCode() + "|" + (mo.getFlowKey() == null ? "" : mo.getFlowKey())).getBytes()));
							map.put(InternationalResource.get(name), url);
						}
					}
				}
			}
		}
		return map;
	}

}