package com.supcon.supfusion.configuration.workflow.service.impl;

import com.supcon.supfusion.configuration.services.entity.EntityTableInfo;
import com.supcon.supfusion.configuration.workflow.dao.EntityTableInfoDaoImpl;
import com.supcon.supfusion.configuration.workflow.service.EntityTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/25
 */
@Service
public class EntityTableInfoServiceImpl implements EntityTableInfoService {

    @Autowired
    private EntityTableInfoDaoImpl entityTableInfoDao;
    @Override
    public EntityTableInfo getITableInfo(Long tableId) {
        return entityTableInfoDao.load(tableId);
    }
}
