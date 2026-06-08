/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.CustomCode;
import org.hibernate.criterion.Criterion;

import java.util.List;

/**
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
public interface CustomCodeService {

	CustomCode getCustomCode(String code);

	void save(CustomCode... customCodes);

	CustomCode getCustomCode(String moduleCode, String modelCode, String type, String subType);

	List<CustomCode> findCustomCodes(String entityCode);

	/**
	 * @param path
	 * @return
	 */
	String getFileContent(String path);

	/**
	 * 
	 * @param codeContent
	 * @param moduleCode
	 *            TODO
	 * @param publishEnabled
	 *            TODO
	 */
	void saveFileContent(String codeContent, String jses5, String originalPath, String entityCodes, Boolean publishEnabled);

	/**
	 * @param entityCode
	 * @return
	 */
	String buildCustomCodeTree(String entityCode, String basePath, Long id, int type, String filterName);

	List<CustomCode> getCustomCodes(String moduleCode);

	List<CustomCode> getCustomCodesByModelCode(String modelCode);

	List<CustomCode> getCustomCodes(Criterion... criterions);
	
	/**
	 * 删除该模块下的所有自定义代码区域
	 * @param customCodes
	 *       
	 */
	void batchDelete(String moduleCode);
	/**
	 * sql批量插入
	 * @param customCodes
	 *       
	 */
	void batchSave(List<CustomCode> customCodes);
	/**
	 * 删除已存在的自定义代码区域
	 * @param list
	 *       
	 */
	 void batchDeleteByCodes(List<String> list);
	 
	 /**
	  * 代码编译和代码热部署
	  * @param path
	  * @param entityCode
	  */
	 void compileAndDeploy(String path, String entityCode);
	 /**
		 * BAP代码简单热部署
		 * @param path
		 * @param module
		 */
	 void bapCodeDeploy(String targetPath, String packagePath);
}
