package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.DepartmentWorkDaoImpl;
import com.supcon.supfusion.base.entities.DepartmentWork;
import com.supcon.supfusion.base.services.DepartmentWorkService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class DepartmentWorkServiceImpl implements DepartmentWorkService {

    @Autowired
    private DepartmentWorkDaoImpl departmentWorkDao;

    @Override
    public Page<DepartmentWork> getByPage(Page<DepartmentWork> page, DetachedCriteria detachedCriteria) {
        return departmentWorkDao.findByPage(page, detachedCriteria);
    }

}
