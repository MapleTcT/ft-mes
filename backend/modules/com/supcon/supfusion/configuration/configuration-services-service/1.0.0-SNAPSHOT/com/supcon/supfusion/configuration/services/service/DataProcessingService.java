/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;

/**
 * 
 * 实体配置 数据处理使用
 * @author zhuyuyin
 * @version 1.0
 */
public interface DataProcessingService {

	void addModuleCodeToAllObject();
	/**
	 * 初始化fullPathName字段
	 */
	void initFullPathNameForTree();
	/**
	 * 初始化leaf字段
	 */
	void initLeafForTree();
	/**
	 * 
	 */
	void dealListPtForAssId();
	/**
	 * 
	 */
	void initVersionProperty();

}
