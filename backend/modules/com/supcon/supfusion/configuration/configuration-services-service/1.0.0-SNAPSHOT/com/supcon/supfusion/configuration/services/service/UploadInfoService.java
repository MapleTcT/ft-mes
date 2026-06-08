/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.UploadInfo;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.util.List;


/**
 * 
 * 上载记录service接口类
 * 
 * @author 朱诗璋
 * @version 1.0
 */
public interface UploadInfoService {

	Page<UploadInfo> findUploadInfoPage(Page<UploadInfo> page, String condition);
	List<UploadInfo> findUploadInfo(String condition);
	void save(UploadInfo uploadInfo);
}
