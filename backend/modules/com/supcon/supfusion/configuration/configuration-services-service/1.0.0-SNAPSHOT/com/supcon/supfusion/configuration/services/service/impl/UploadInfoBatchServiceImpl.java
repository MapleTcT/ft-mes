/**
 * Copyright c SUPCON CORPORATION 2011 All Rights Reserved.
 * 
 * DepartmentServiceImpl.java
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.configuration.services.entity.UploadInfoBatch;
import com.supcon.supfusion.configuration.services.dao.UploadInfoBatchDaoImpl;
import com.supcon.supfusion.configuration.services.dao.UploadInfoDaoImpl;
import com.supcon.supfusion.configuration.services.service.UploadInfoBatchService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
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
@Service("uploadInfoBatchService")
@Transactional
public class UploadInfoBatchServiceImpl implements UploadInfoBatchService {
	@Autowired
	private UploadInfoBatchDaoImpl uploadInfoBatchDao;
	@Autowired
	private UploadInfoDaoImpl uploadInfoDao;

	@Override
	public void update(UploadInfoBatch uploadInfoBatch) {
		uploadInfoBatchDao.update(uploadInfoBatch);
	}

	@Override
	public void save(UploadInfoBatch uploadInfoBatch) {
		uploadInfoBatchDao.save(uploadInfoBatch);
	}

	@Override
	public Page<UploadInfoBatch> findUploadInfoBatchPage(Page<UploadInfoBatch> page, DetachedCriteria detachedCriteria) {
		Criteria criteria=uploadInfoBatchDao.createCriteria(detachedCriteria);
		criteria.addOrder(Order.desc("uploadDate"));
		return uploadInfoBatchDao.findByPage(page, criteria);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public boolean updateHistoryData() {
		// TODO Auto-generated method stub
		StringBuffer hql = new StringBuffer("from UploadInfo UploadInfo");
		hql.append(" where UploadInfo.uploadInfoBatch is null ");
		List<UploadInfo> uploadInfoList = uploadInfoBatchDao.createQuery(hql.toString()).list();
		if(uploadInfoList.size() > 0){
			for(UploadInfo uploadinfo : uploadInfoList){
				UploadInfoBatch batch = new UploadInfoBatch();
				batch.setDescribe(uploadinfo.getModuleName());
				batch.setTotalTime(uploadinfo.getTotalTime());
				batch.setModuleSize(1);
				batch.setUploadStaff(uploadinfo.getUploadStaff());
				if("上载成功".equals(uploadinfo.getUploadState())){
					batch.setUploadState("success");
				}else{
					batch.setUploadState("fail");
				}
				batch.setUploadDate(uploadinfo.getUploadDate());
				uploadInfoBatchDao.create(batch);
				uploadInfoBatchDao.flush();
				uploadinfo.setUploadInfoBatch(batch);
				uploadInfoDao.update(uploadinfo);
			}
		}
		return false;
	}
	
}
