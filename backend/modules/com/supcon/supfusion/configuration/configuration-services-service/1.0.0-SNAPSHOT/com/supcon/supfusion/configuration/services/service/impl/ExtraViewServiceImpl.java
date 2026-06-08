package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.ExtraView;
import com.supcon.supfusion.configuration.services.dao.ViewDaoImpl;
import com.supcon.supfusion.configuration.services.service.ExtraViewService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ServiceApiService("ec_ExtraViewService")
@Transactional
public class ExtraViewServiceImpl implements ExtraViewService {

	@Autowired
	private ViewDaoImpl viewDao;
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ExtraView getExtraView(String code) {
		return viewDao.getExtraView(code);
	}
}
