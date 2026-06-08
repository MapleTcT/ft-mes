/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.Role;

import java.util.List;

/**
 * 
 * @author 曹伟彪
 * 
 */

public interface RoleService {

	Role load(Long id);

	List<Role> getTreeChildren(Long roleId, Long companyId);

}

