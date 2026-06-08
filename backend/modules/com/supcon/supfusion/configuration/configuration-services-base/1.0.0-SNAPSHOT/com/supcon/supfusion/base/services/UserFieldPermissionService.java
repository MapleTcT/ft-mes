/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;


import java.util.Map;

/**
 * comments add by DHY 2011-09-20
 * 这里的findEditPermission，执行反逻辑的，即他查找Edit时，是找permit<3的字段权限;
 * 查找View时，是找permit<1的字段权限
 */
public interface UserFieldPermissionService {

	int findFieldPermission(String modelCode, String propertyKey, String propertyCode);

	Map<String, Integer> getNoPermissionFieldMap(String modelCode, String keys);

}
