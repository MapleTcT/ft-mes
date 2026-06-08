package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.DepartmentWork;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import org.hibernate.criterion.DetachedCriteria;

public interface DepartmentWorkService {
    Page<DepartmentWork> getByPage(Page<DepartmentWork> departmentWorkPage, DetachedCriteria detachedCriteria);
}
