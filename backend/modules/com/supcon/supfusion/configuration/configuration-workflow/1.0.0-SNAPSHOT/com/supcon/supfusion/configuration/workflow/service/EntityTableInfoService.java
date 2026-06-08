/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.workflow.service;


import com.supcon.supfusion.configuration.services.entity.EntityTableInfo;

/**
 * @author rockey
 * 
 */
public interface EntityTableInfoService {

	EntityTableInfo getITableInfo(Long tableId);
}
