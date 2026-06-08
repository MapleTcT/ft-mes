/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.enums;

/**
 * 扩展点区域类型
 * 
 * @author zhuyuyin
 * @version 1.0
 */
public enum ZoneType {
	/**
	 * 主界面
	 */
	MAINFRAME("foundation.extension.zone.mainFrame"),
	/**
	 * 个性化设置区域
	 */
	PERSONALIZE("foundation.extension.zone.personalize");
	
	
	private String value;
	private ZoneType(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}
}
