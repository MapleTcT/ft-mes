package com.supcon.supfusion.i18n.common.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class I18nQueryDTO {
	
	private String tenantId;
	/**
	 * 国际化key
	 */
	private String[] i18nKeys;
	/**
	 * 是否联想查询
	 */
	private boolean associate;
	/**
	 * {'zh_CN': '下载', 'zh_HK': 'ff'}
	 */
	private Map<String, List<String>> languageMap;
}
