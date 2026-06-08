package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.UserDaoImpl;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.services.UserService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/8/13
 */
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDaoImpl userDao;

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public User load(Long id) {
        return userDao.load(id);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public User getUserByUsername(String name) {
        List<User> list = userDao.findByCriteria(Restrictions.eq("name", name), Restrictions.eq("valid", true));
        if(list.size()==0){
            return null;
        }
        return list.get(0);
    }

    @Override
    public Page<User> getByPage(Page<User> page, String hql, Object... objects) {
        return userDao.findByPage(page, hql, objects);
    }
}
