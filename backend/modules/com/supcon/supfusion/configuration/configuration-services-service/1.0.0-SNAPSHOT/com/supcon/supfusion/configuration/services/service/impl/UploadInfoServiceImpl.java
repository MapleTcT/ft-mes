/**
 * Copyright c SUPCON CORPORATION 2011 All Rights Reserved.
 * 
 * DepartmentServiceImpl.java
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.configuration.services.dao.UploadInfoDaoImpl;
import com.supcon.supfusion.configuration.services.service.UploadInfoService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 上载记录service接口类
 * 
 * @author 朱诗璋
 * @version 1.0
 */
@Service("uploadInfoService")
@Transactional
public class UploadInfoServiceImpl implements UploadInfoService {

	@Autowired
	private UploadInfoDaoImpl uploadInfoDao;

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Page<UploadInfo> findUploadInfoPage(Page<UploadInfo> page, String condition) {
		// TODO Auto-generated method stub
		StringBuffer hql = new StringBuffer("from UploadInfo uploadInfo");
		hql.append(condition);
		hql.append(" order by uploadInfo.id desc");
		return uploadInfoDao.findByPage(page, hql.toString());
	}
	
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<UploadInfo> findUploadInfo(String condition) {
		// TODO Auto-generated method stub
		StringBuffer hql = new StringBuffer("from UploadInfo uploadInfo where 1=1 ");
		hql.append(condition);
		hql.append(" order by uploadInfo.id desc");
		return uploadInfoDao.findByHql(hql.toString(), new Object[]{});
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(UploadInfo uploadInfo) {
		uploadInfoDao.save(uploadInfo);
	}


}
