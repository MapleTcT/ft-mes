/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;


import java.util.Map;

/**
 * @author rockey
 * 
 */
public interface SystemCodeInfoService{
	/**
	 * 根据公司按模块分组获取模块下的系统编码
	 * @param company
	 * @param language 
	 * @return
	 */
	Map<String, Map<String, Map<String, String>>> getSystemEntityMapByGroup(String moduleCode);

}
