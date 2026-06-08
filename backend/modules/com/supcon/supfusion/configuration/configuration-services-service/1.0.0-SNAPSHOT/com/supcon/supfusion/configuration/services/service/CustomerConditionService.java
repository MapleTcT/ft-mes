package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.CustomerCondition;
import com.supcon.supfusion.configuration.services.entity.DataClassific;
import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.View;
import org.hibernate.criterion.Criterion;

import java.util.List;

public interface CustomerConditionService {
	
	public void saveCustomerCondition(CustomerCondition condition);
	
	public CustomerCondition getCustomerCondition(View view);
	
	public CustomerCondition getCustomerCondition(DataGrid datagrid);
	
	public CustomerCondition getCustomerCondition(DataClassific dataClassific);
	
	public CustomerCondition getCustomerConditionByDataGridCode(String dataGridCode);
	
	public CustomerCondition getCustomerConditionByViewCode(String viewCode);
	
	public CustomerCondition getCustomerConditionByClassificCode(String classificCode);

	/**
	 * 根据传入对象物理删除自定义条件
	 * @param object  只能为 {@link View}  {@link DataGrid}  {@link DataClassific}
	 */
	void deletePhysicalByObject(Object object);
	/**
	 * 根据传入对象逻辑删除自定义条件
	 * @param object  只能为 {@link View}  {@link DataGrid}  {@link DataClassific}
	 */
	void deleteByObject(Object object);

	/**
	 * 根据{@link CustomerCondition}.code的起始部分查询{@link CustomerCondition}
	 * @param startCode code起始部分
	 * @return
	 */
	List<CustomerCondition> findCustomerConditionsByCode(String startCode);

	/**
	 * 根据{@link CustomerCondition}.code的查询{@link CustomerCondition} 
	 * 一级缓存
	 * @param code 
	 * @return {@link CustomerCondition}
	 */
	CustomerCondition findCustomerCondition(String code);
	/**
	 * 根据条件查询CustomerCondition
	 * @param criterions
	 * @return
	 */
	List<CustomerCondition> findCustomerConditions(Criterion... criterions);
	
}
