/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询对于实体是否有字段权限配置的权限
 * 
 * @author wuxinhui
 */
@Component
public class GetEnableFieldsPermissionConfigMethod implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		int enablePermit = 0;// 0 false 1 true

		return enablePermit;
	}
}
