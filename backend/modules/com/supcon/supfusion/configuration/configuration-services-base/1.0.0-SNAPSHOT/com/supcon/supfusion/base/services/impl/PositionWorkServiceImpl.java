package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.PositionWorkDaoImpl;
import com.supcon.supfusion.base.entities.PositionWork;
import com.supcon.supfusion.base.services.PositionWorkService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional
public class PositionWorkServiceImpl implements PositionWorkService {

    @Autowired
    private PositionWorkDaoImpl positionWorkDao;

    @Override
    public Page<PositionWork> getByPage(Page<PositionWork> page, DetachedCriteria detachedCriteria) {
        return positionWorkDao.findByPage(page, detachedCriteria);
    }
}
