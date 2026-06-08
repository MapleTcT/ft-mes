/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.UserPermission;

public interface UserPermissionService {


	UserPermission findPermissionByOperateCodeAndUserId(MenuOperate operate, Long userId);
}
