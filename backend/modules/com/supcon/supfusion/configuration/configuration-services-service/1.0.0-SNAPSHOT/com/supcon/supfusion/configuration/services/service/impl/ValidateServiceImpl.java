package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.ValidateDaoImpl;
import com.supcon.supfusion.configuration.services.entity.Validate;
import com.supcon.supfusion.configuration.services.service.ValidateService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 配置信息验证实现
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@ServiceApiService("ec_ValidateService")
@Transactional
public class ValidateServiceImpl implements ValidateService {

	@Autowired
	private ValidateDaoImpl validateDao;

	@Override
	@Transactional
	public void saveValidate(Validate validate) {
		validateDao.flush();
		if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
			validate.setProjFlag(true);
		}
		validateDao.save(validate);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Validate getValidate(String validateCode) {
		return validateDao.load(validateCode);
	}

	@Override
	@Transactional
	public void deleteValidate(Validate validate) {
		validateDao.deletePhysical(validate);
	}

	@Override
	@Transactional
	public void deleteValidate(String validateCode) {
		Validate validate = getValidate(validateCode);
		if (null != validate) {
			validateDao.deletePhysical(validate);
		}
	}

}
