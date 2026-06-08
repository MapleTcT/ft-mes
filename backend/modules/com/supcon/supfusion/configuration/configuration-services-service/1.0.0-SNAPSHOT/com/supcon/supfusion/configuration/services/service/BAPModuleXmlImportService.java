/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;


import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.entity.ModuleRelation;
import org.hibernate.SessionFactory;

import java.util.List;

/**
 * XML实体配置Runtime和Ec导入接口
 * 
 * @author fangzhibin
 * @version $Id$
 */
public interface BAPModuleXmlImportService {

	/**
	 * 导入功能
	 * @param xml
	 * @param uploadWorkFlow
	 * @param sessionFactory
	 * @param filter TODO
	 * @param env TODO
	 * @return
	 */
	public Module importXml(String xml, Boolean uploadWorkFlow, SessionFactory sessionFactory, boolean filter, String... env);

}