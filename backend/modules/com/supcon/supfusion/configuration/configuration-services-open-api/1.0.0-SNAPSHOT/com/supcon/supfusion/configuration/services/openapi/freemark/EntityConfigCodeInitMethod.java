/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.freemark;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 
 * 
 * @author zhuyuyin
 * @version $Id$
 */
@Component
public class EntityConfigCodeInitMethod implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		String codeType = "cell";
		if (null != arguments && !arguments.isEmpty() && null != arguments.get(0)) {
			codeType = arguments.get(0).toString();
		} 
		return codeType + "_" + new Date().getTime() + "_" + Math.round(Math.random() * 10000);
	}

}
