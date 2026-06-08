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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * datatable中获取没有权限的字段Map
 * <p>
 * 以提供datatable在渲染的时候能够对没有权限的字段进行不同的样式展现
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Component
public class GetPermissionFieldsMapMethod implements TemplateMethodModelEx {

	@Autowired
	UserFieldPermissionService userFieldPermissionService;
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Map<String, Integer> noPermissionFieldMap = new HashMap<String, Integer>();
		String keys = null, modelCode = null;
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0) && null != arguments.get(1)) {
			modelCode = arguments.get(0).toString();
			keys = arguments.get(1).toString();
			noPermissionFieldMap = userFieldPermissionService.getNoPermissionFieldMap(modelCode, keys);
		}
		return noPermissionFieldMap;
	}

}
