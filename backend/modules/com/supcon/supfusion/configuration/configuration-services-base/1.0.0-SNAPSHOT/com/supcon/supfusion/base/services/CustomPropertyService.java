package com.supcon.supfusion.base.services;

import java.util.List;

/**
 * @author wuqi
 *
 */
public interface CustomPropertyService {

	/**
	 * 查找指定业务值的自定义字段
	 * 
	 * @param modelCode
	 *            当前模型
	 * @param businessModel
	 *            业务模型
	 * @param businessValue
	 *            业务值
	 * @return 返回List<CustomPropertyViewMapping>
	 */
	@SuppressWarnings("rawtypes")
	List findCPByBusinessValue(String modelCode, String businessModel, Object... businessValue);

}
