package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.UserPermissionDaoImpl;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.UserPermission;
import com.supcon.supfusion.base.services.UserPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/23
 */
@Slf4j
@Service
@Transactional
public class UserPermissionServiceImpl implements UserPermissionService {

    @Autowired
    private UserPermissionDaoImpl userPermissionDao;

    @Override
    public UserPermission findPermissionByOperateCodeAndUserId(MenuOperate operate, Long userId) {
        List<UserPermission> ups = userPermissionDao.findByCriteria(Restrictions.eq("user.id", userId), Restrictions.eq("menuOperate", operate));
        if (ups != null && ups.size() > 0) {
            return ups.get(0);
        }
        return null;
    }
}
