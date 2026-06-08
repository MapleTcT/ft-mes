package com.supcon.supfusion.custon.property.server;


import com.supcon.supfusion.custon.property.dao.entity.DataGrid;

public interface EcConfigService {
	/**
	 * 获取完整的视图配置信息
	 * 
	 * @param object
	 * @return
	 */
	String getEcFullConfig(Object object);

	/**
	 * 获取视图字段属性信息
	 * 
	 * @param object
	 * @return xml String
	 */
	String getFieldsConfig(Object object);

	/**
	 * 根据DataGrid获取关联视图的字段属性Map
	 * 
	 * @param dataGrid
	 * @return
	 */
	String getViewFieldConfigByDataGrid(DataGrid dataGrid);

}
