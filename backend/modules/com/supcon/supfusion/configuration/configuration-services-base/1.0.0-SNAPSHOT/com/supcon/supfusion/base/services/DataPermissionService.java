/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.DataPermission;
import com.supcon.supfusion.base.entities.DataPermissionStaff;
import com.supcon.supfusion.base.entities.DataPmsPosition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author qy
 * 
 */
public interface DataPermissionService {

	Set<Map<String, Object>> getFlowStart(String entityCode, Long userId);

	String getFlowPower(String flowKey,String flowVersion);

	void updateMenuUserInfo(String flowKey,String flowVersion,String activeArr,String operatePowers,String entityCode,Long menuId);

	void saveWorkFlowPermissionChanges(Long deploymentId,String updatePowerString);

	List<Long> getPowerUserList(Long processInitiator, String activeCode, String processKey, String valueOf, Long initiatorPositionId, String groupIds, Boolean crossCompanyFlag);

	//保存permission
	void savePermission(DataPermission dataPermission);

	//保存permissionStaff
	void savePermissionStaff(DataPermissionStaff dataPermissionStaff);

	//保存permissionPosition
	void savePermissionPosition(DataPmsPosition dataPmsPosition);
}
