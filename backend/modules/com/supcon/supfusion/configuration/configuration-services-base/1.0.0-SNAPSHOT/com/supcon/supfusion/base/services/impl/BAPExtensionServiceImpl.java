package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.BAPExtensionDaoImpl;
import com.supcon.supfusion.base.entities.BAPExtension;
import com.supcon.supfusion.base.services.BAPExtensionService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class BAPExtensionServiceImpl implements BAPExtensionService {

    @Autowired
    private BAPExtensionDaoImpl bapExtensionDao;
    @Override
    public void save(BAPExtension entity) {

    }

    @Override
    public BAPExtension getExtension(String code) {
        return bapExtensionDao.findEntityByCriteria(Restrictions.eq("code", code));
    }
}
