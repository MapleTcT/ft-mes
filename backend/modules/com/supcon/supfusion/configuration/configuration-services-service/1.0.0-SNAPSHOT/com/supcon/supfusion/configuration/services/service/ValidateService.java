package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.Validate;

/**
 * 配置信息验证操作接口
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
public interface ValidateService {

	/**
	 * 保存验证
	 * @param Validate
	 */
	void saveValidate(Validate validate); 
	
	
	/**
	 * 获取验证
	 * @param ValidateCode
	 * @return
	 */
	Validate getValidate(String validateCode);
	
	/**
	 * 删除验证
	 * @param Validate
	 */
	void deleteValidate(Validate validate);
	
	/**
	 * 
	 * @param ValidateCode
	 */
	void deleteValidate(String validateCode);

}
