package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.Layout;
import com.supcon.supfusion.configuration.services.dao.LayoutDaoImpl;
import com.supcon.supfusion.configuration.services.service.LayoutService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ServiceApiService
@Transactional
public class LayoutServiceImpl implements LayoutService {
	@Autowired
	private LayoutDaoImpl layoutDao;

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Layout> findAll() {
		return layoutDao.findByCriteria();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Layout get(String code) {
		return layoutDao.get(code);
	}

}
