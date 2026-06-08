package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.RolePSpecialPermissionDaoImpl;
import com.supcon.supfusion.base.entities.RolePSpecialPermission;
import com.supcon.supfusion.base.services.RolePSpecialPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class RolePSpecialPermissionServiceImpl implements RolePSpecialPermissionService {

    @Autowired
    private RolePSpecialPermissionDaoImpl rolePSpecialPermissionDao;

    @Override
    public RolePSpecialPermission load(Long id) {
        return rolePSpecialPermissionDao.get(id);
    }
}
