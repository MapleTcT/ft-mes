package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Button;
import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.View;
import org.hibernate.criterion.Criterion;

import java.util.List;
import java.util.Map;

/**
 * 配置信息按钮操作接口
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
public interface ButtonService {

	/**
	 * 保存按钮
	 * @param button
	 */
	void saveButton(Button button); 
	
	void mergeButton(Button button);
	
	/**
	 * 获取按钮
	 * @param buttonCode
	 * @return
	 */
	Button getButton(String buttonCode);
	
	/**
	 * 
	 * @param button
	 * @return
	 */
	Button getButton(Button button);
	
	/**
	 * 删除按钮
	 * @param button
	 */
	void deleteButton(Button button);
	
	/**
	 * 
	 * @param buttonCode
	 */
	void deleteButton(String buttonCode);

	/** 
	 * 保存按钮
	 * @param object
	 * @param fieldConfig
	 * @param btDelCellIds
	 */
	void saveButton(Object object, String fieldConfig, String btDelCellIds);

	Map<String, Button> getButtons(View view);

	List<Button> getButtons(String viewCode);

	void deleteButtonByCellCodes(Object obj, String cellCodes);

	Map<String, Button> getButtons(DataGrid dataGrid);

	List<Button> getButtonsByDataGridCode(String dataGridCode);
	/**
	 * 处理Button的操作类型
	 */
	void addOperateType(String moduleCode);

	/**
	 * @param viewCode
	 * @return
	 */
	List<Button> getButtonsByViewSelect(String viewCode);

	/**
	 * @param viewCode
	 */
	void deleteButtonByViewCode(String viewCode);

	/**
	 * @param dataGridCode
	 */
	void deleteButtonByDataGridCode(String dataGridCode);
	/**
	 * 根据条件查询Button
	 * @param criterions
	 * @return
	 */
	List<Button> findButtons(Criterion... criterions);
}
