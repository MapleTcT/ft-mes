package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.AdvQueryCondition;
import com.supcon.supfusion.configuration.services.entity.DefaultAdvCond;

import java.util.List;


/**
 * 条件实体业务逻辑类
 * 
 * @author 谭正阳
 * 
 */
public interface ConditionService {

	/**
	 * 把条件转化为SQL
	 * 
	 * @param advQueryCond
	 * @return AdvQueryCondition
	 * @throws Exception
	 */
	AdvQueryCondition toSql(String advQueryCond, Boolean... existsParam);

	/**
	 * 获取条件
	 * 
	 * @param viewCode
	 * @return
	 */
	DefaultAdvCond getDefaultAdvCond(String viewCode);

	/**
	 * 保存条件
	 * 
	 * @param viewCode
	 * @return
	 */
	void saveDefaultAdvCond(DefaultAdvCond defaultAdvCond);


	/**
	 * 获取视图对应的条件
	 *
	 * @param viewCode
	 * @return
	 */
	List<AdvQueryCondition> getAdvQueryConditionByView(String viewCode);

}
