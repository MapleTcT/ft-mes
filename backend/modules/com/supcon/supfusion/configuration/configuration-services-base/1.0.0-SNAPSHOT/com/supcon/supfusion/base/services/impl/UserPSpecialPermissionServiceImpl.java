package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.entities.UserPSpecialPermission;
import com.supcon.supfusion.base.services.UserPSpecialPermissionService;
import lombok.extern.slf4j.Slf4j;
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
public class UserPSpecialPermissionServiceImpl implements UserPSpecialPermissionService {
    @Override
    public UserPSpecialPermission load(Long id) {
        return null;
    }
}
