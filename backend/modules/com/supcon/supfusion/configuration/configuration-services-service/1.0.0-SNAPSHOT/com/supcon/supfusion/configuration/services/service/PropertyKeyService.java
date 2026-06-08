/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;


/**
 * 验证数据库字段和java关键字
 * @author qy
 *
 */
public interface PropertyKeyService {
	/**
	 * 检测java关键字
	 * @author qianyong
	 * @param key  java变量
	 * @return 是关键字返回ture，否则返回false
	 * */
	public Boolean checkJavaKey(String key);
	/**
	 * 检测数据库关键字
	 * @author qianyong
	 * @param key  字段
	 * @return 是关键字返回ture，否则返回false
	 * */
	public Boolean checkDBKey(String colName);
	/**
	 * 检测java关键字(供字段使用，字段与模型检测分开，二者使用场合不一样)
	 * @author tanzhengyang
	 * 
	 * @param key  字段
	 * @return 是关键字返回ture，否则返回false
	 * */
	Boolean checkPropertyKey(String key);
	
	/**
	 * 检测bap关键字
	 * @author huning
	 * @param key  
	 * @return 是关键字返回ture，否则返回false
	 * */
	public Boolean checkBapKey(String key);
}
