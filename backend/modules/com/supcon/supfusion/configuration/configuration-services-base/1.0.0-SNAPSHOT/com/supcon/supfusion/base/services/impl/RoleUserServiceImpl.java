package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.RoleUserDaoImpl;
import com.supcon.supfusion.base.dao.UserDaoImpl;
import com.supcon.supfusion.base.entities.RoleUser;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.services.RoleUserService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
public class RoleUserServiceImpl implements RoleUserService {

    @Autowired
    private UserDaoImpl userDao;
    @Autowired
    private RoleUserDaoImpl roleUserDao;

    @Override
    public List<Long> getRoleUserByUserId(Long userId) {
        User user = userDao.load(userId);
        if (null != user) {
            List<RoleUser> list = this.getRoleUserByUser(user);
            if (null != list && !list.isEmpty()) {
                List<Long> ids = new ArrayList<Long>();
                for (int i = 0; i < list.size(); i++) {
                    long l = list.get(i).getRole().getId().longValue();
                    if (!ids.contains(l)) {
                        ids.add(l);
                    }
                }
                return ids;
            }
        }
        return null;
    }

    @Override
    public Page<RoleUser> getByPageFilterRole(Page<RoleUser> page, DetachedCriteria detachedCriteria) {
        return roleUserDao.findByPage(page, detachedCriteria);
    }

    public List<RoleUser> getRoleUserByUser(User user) {
        List<RoleUser> retList = null;
        retList = roleUserDao.findByHql("from RoleUser as ru where ru.valid=true and  ru.user=?", user);
        return retList;
    }
}
