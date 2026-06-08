package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.RoleDaoImpl;
import com.supcon.supfusion.base.dao.UserDaoImpl;
import com.supcon.supfusion.base.entities.Role;
import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.base.services.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleDaoImpl roleDao;
    @Override
    public Role load(Long id) {
        return roleDao.load(id);
    }

    @Override
    public List<Role> getTreeChildren(Long roleId, Long companyId) {
        return null;
    }
}
