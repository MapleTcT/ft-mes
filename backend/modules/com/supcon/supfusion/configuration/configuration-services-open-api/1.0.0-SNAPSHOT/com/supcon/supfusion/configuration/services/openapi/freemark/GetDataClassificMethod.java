/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.service.ViewService;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Component
public class GetDataClassificMethod implements TemplateMethodModelEx {

	@Autowired
	private ViewService viewService;
	/*
	 * (non-Javadoc)
	 * 
	 * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Object dgObj = null;
		if (null != arguments && arguments.size() > 0) {
			String viewCode = (String) arguments.get(0);
			if (null != viewCode && viewCode.length() > 0) {
				if (arguments.size() == 2) {
					dgObj = viewService.getDefaultDataClassific(viewCode);
				} else {
					if(arguments.size() == 3){
						String layoutName = (String) arguments.get(1);
						if(null != layoutName && layoutName.length() > 0 && arguments.size() == 3) {
							dgObj = viewService.getDataClassificByViewCode(viewCode,layoutName);
						} else {
							dgObj = viewService.getDataClassificByViewCode(viewCode);
						}
					}else{
						dgObj = viewService.getDataClassificByViewCode(viewCode);
					}
				}
			}
		}
		return dgObj;
	}
}
