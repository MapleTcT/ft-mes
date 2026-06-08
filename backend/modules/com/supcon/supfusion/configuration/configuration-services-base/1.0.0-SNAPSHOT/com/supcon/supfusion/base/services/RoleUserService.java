/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.RoleUser;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

import java.util.List;

/**
 * 
 * @author 赵程遥
 * 
 */

public interface RoleUserService {

	 /**
	 * 根据user ID 查询对应的角色ID
	 * @aram user
	 * @return
	 */
	List<Long> getRoleUserByUserId(Long userId);

	Page<RoleUser> getByPageFilterRole(Page<RoleUser> page,
									   DetachedCriteria detachedCriteria);

}
