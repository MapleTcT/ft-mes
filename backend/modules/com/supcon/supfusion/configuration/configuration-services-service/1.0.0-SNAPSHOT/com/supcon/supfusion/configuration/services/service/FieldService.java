package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.Field;
import com.supcon.supfusion.configuration.services.entity.SelectionRange;
import com.supcon.supfusion.configuration.services.entity.View;
import org.hibernate.criterion.Criterion;

import java.util.List;
import java.util.Map;

/**
 * 配置信息字段处理接口
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
public interface FieldService {

	/**
	 * 保存字段
	 * 
	 * @param field
	 */
	void saveField(Field field);

	/**
	 * 获取字段
	 * 
	 * @param fieldCode
	 * @return
	 */
	Field getField(String fieldCode);

	/**
	 * 删除字段
	 * 
	 * @param field
	 */
	void deleteField(Field field);

	/**
	 * 
	 * @param fieldCode
	 */
	void deleteField(String fieldCode);

	/**
	 * 保存视图中所有字段
	 * 
	 * @param view
	 * @param fieldConfig
	 * @param delCellIds
	 */
	void saveFields(Object object, String fieldConfig, String delCellIds, String delEventIds, String delValidateIds);

	/**
	 * 根据视图查找字段
	 * 
	 * @param view
	 * @return
	 */
	Map<String, Field> getFields(View view);

	/**
	 * 根据视图CODE查找字段
	 * 
	 * @param view
	 * @return
	 */
	List<Field> getFields(String viewCode);

	List<Field> findFields(String viewCode);

	void deleteFieldByCellCodes(String code, String cellCodes);

	/**
	 * 根据DataGrid查找字段
	 * 
	 * @param view
	 * @return
	 */
	Map<String, Field> getFields(DataGrid dataGrid);
	/**
	 * 根据DataGrid CODE查找字段
	 * 
	 * @param view
	 * @return
	 */
	List<Field> getFieldsByDataGridCode(String dataGridCode);
	/**
     * 根据FastQueryJson CODE查询字段
     *
     * @param fqjCode
     * @return
     */
    List<Field> getFieldByFastQueryJsonCode(String fqjCode);

    /**
     * 根据AdvQueryJson CODE查询字段
     *
     * @param aqjCode
     * @return
     */
    List<Field> getFieldByAdvQueryJsonCode(String aqjCode);
    
	void deleteFieldByDataGrid(String dgCode);

	void deleteFieldByViewCode(String viewCode);

	Field getFieldByDgCode(String dgCode, String viewCode);

	/**
	 * 处理field code 按新规则组织
	 * @param moduleCode
	 */
	void modifyFieldCode(String moduleCode);

	/**
	 * @param propertyCode
	 * @return
	 */
	List<Field> getFieldByPropertyCode(String propertyCode);
	/**
	 * 根据对象类型的propertyCode查找field，fullPropertyCodJSONObject json = new JSONObject();e以propertyCode开头
	 * @param propertyCode
	 * @return
	 */
	List<Field> getFieldByPropertyCodeLike(String propertyCode);
	/**
	 * 对所有Field进行数据处理 添加 fullPropertyCode字段
	 */
	void dealFieldData();
	/**
	 * 清除指定缓存
	 * @param key
	 */
	void clearCache(Object key);
	/**
	 * 清除所有缓存
	 */
	void clearCache();
	/**
	 * 根据条件查询Field
	 * @param criterions
	 * @return
	 */
	List<Field> findFields(Criterion... criterions);
	
	void saveSelectionRange(SelectionRange range);
	
	void deleteSelectionRange(Long id);
	
	void deleteSelectionRangeByField(Field field);
	
	SelectionRange getSelectionRangeById(Long id);
	
	List<SelectionRange> getSelectionRangeByFieldCode(String fieldCode);

	/**
	 * 对所有Field进行数据处理 添加columnType字段
	 */
	void dealFieldColumnType();
	
	Field findFieldByCellCode(String cellCode, View view);

	/**
	 * @param entityCodes
	 * 更新Field的config字段
	 */
	void updateFieldsByEntityCodes(String entityCodes);

	List<SelectionRange> findSelectionRanges(Criterion... criterions);
	
}
