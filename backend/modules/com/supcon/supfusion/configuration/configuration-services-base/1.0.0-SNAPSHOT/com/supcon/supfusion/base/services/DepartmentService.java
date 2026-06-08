/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.services;


import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

import java.util.List;


public interface DepartmentService {



	/**
	 * 查询符合条件的部门的LayRec列表 <br>
	 * 
	 * @param deptName
	 *            查询条件：部门名称
	 * @param companyId
	 *            查询条件：部门所属公司ID
	 * @return 符合条件的部门的LayRec列表
	 */
	List<String> getDepartmentChildren(String deptName, Long companyId);

	Department load(Long id);

	List<Department> getTreeChildren(Long id, Long companyId);

	Page getByPage(Page<Department> departmentPage, DetachedCriteria detachedCriteria);
}
