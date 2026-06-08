/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.UploadInfoBatch;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

/**
 * 
 * 上载批量记录service接口类
 * 
 * @author 朱诗璋
 * @version 1.0
 */
public interface UploadInfoBatchService {

	void update(UploadInfoBatch uploadInfoBatch);
	void save(UploadInfoBatch uploadInfoBatch);
	Page<UploadInfoBatch> findUploadInfoBatchPage(Page<UploadInfoBatch> page, DetachedCriteria detachedCriteria);
	boolean updateHistoryData();
}
