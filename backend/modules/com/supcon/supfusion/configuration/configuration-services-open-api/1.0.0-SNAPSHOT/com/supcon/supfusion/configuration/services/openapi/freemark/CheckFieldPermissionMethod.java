/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.services.UserFieldPermissionService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 判断字段是否有权限查看
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Component
public class CheckFieldPermissionMethod implements TemplateMethodModelEx {

	@Autowired
	private UserFieldPermissionService userFieldPermissionService;

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		int permit = 3;
		String propertyKey = null, modelCode = null, propertyCode = null;
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0) && null != arguments.get(1) && null != arguments.get(2)) {
			propertyKey = arguments.get(0).toString();
			modelCode = arguments.get(1).toString();
			propertyCode = arguments.get(2).toString();
			permit = userFieldPermissionService.findFieldPermission(modelCode, propertyKey, propertyCode);
		}
		return permit;
	}
}
