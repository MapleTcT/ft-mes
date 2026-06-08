package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.UserDataPermissionMapper;
import com.supcon.supfusion.rbac.dao.po.UserDataPermissionPO;
import com.supcon.supfusion.rbac.service.IUserDataPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 业务数据权限用户关联表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Slf4j
@Service
@Transactional
public class UserDataPermissionServiceImpl extends ServiceImpl<UserDataPermissionMapper, UserDataPermissionPO> implements IUserDataPermissionService {

}
