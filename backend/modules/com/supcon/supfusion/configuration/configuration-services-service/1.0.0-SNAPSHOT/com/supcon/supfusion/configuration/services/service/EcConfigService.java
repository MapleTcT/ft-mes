package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.View;

import java.util.Map;

/**
 * 
 * 获取完整的视图配置信息 接口
 * 
 * @author zhuyuyin
 * @version $Id$
 */
public interface EcConfigService {

	/**
	 * 获取完整的视图配置信息
	 * @param object
	 * @return
	 */
	String getEcFullConfig(Object object);

	/**
	 * 根据视图获取关联DataGrid的字段属性Map
	 * @param view
	 * @return
	 */
	Map<String, String> getDataGridFieldConfigByView(View view);

	/**
	 * 根据DataGrid获取关联视图的字段属性Map
	 * @param dataGrid
	 * @return
	 */
	String getViewFieldConfigByDataGrid(DataGrid dataGrid);
	/**
	 * 获取视图字段属性信息
	 * @param object
	 * @return xml String
	 */
	String getFieldsConfig(Object object);
	/**
	 * 更改视图或DataGrid的配置信息 层级结构更改并抽取字段属性信息
	 * @param moduleCode
	 */
	void modifyConfiguration(String moduleCode);

	/**
	 * @param moduleCode
	 */
	void dealFieldByFastQueryDateAndButton(String moduleCode);

	/**
	 * 处理党群与人力模块可空非空样式
	 */
	void dealHrAndPartCss();

	/**
	 * deal ec env
	 */
	void dealEcEnv();

}
