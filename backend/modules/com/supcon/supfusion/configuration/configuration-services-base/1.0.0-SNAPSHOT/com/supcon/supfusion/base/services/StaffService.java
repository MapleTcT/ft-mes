/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.DepartmentWork;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.sql.SQLException;
import java.util.Map;

/**
 * 
 * @author 曹伟彪
 * 
 */

public interface StaffService {
    
	/**
	 * 根据ID，获取 部门 对象
	 * @param id
	 * @return
	 */
	Staff get(Long id);

	Staff load(Long staffId);

	Page<Map<String, Object>> findRecordPage(Page<Map<String, Object>> page, String queryResultSQL, Object... objects)
			throws SQLException;

	Page<DepartmentWork> deptfindstaffworkInfo(Page<DepartmentWork> departmentWorkPage);

}

