/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

/**
 * 
 * @author 曹伟彪
 * 
 */

public interface UserService {

	User load(Long id);

	User getUserByUsername(String name);

	Page<User> getByPage(Page<User> page, String hql, Object... objects);
}

