/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.International;
import com.supcon.supfusion.base.entities.Language;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.io.File;
import java.util.List;
import java.util.Map;


public interface InternationalService {

	String getI18nValue(String key);

	String getI18nValue(String key, Object[] args);

	String getI18nValue(String key, Object[] args, String language);

	void addInternational(String key, Map map);

	String addInternational(String key);

	List<International> getInternationals(String key);

	String createNewInternational(String messageKey);

	List<Language> getAllLanguage();

	void initInternational(String moduleCode, File i18nFiles[]);
	void initProjInternational(String moduleCode, File i18nFiles);

	List<String> getI18nKey(String value);

	void internationalPage(Page<Map<String, Object>> page, International example);

	void refreshI18n();

	/**
	 * @Author kk.C
	 * @Description 选中某种类型是创建国际化key(根据模块名称创建国际化key值)  根据模块名称创建国际化key值
	 * @Date 2020/11/19 16:17
	 * @Param [moduleCode]
	 * @return java.lang.String
	 **/
	String initI18nKey(String moduleCode);

	List<String> initI18nKeys(String moduleCode, Integer num);

	/**
	 * 某个模块下的一批国际化资源
	 *
	 * @param map        国际化key 对应的多个语言的 value 模块code
	 *                   {
	 *                   organization.base.calendar.set1898989121212212:组织日历
	 *                   }
	 * @param moduleCode 模块编码  必传
	 * @param language   语言类型  非必传
	 * @return 返回新增结果
	 */
	void messageResourceAddOrUpdateList(Map<String, String> map, String moduleCode, String language);

	/**
	 * 获取当前语言
	 * @return
	 */
	String getLocale();
}
