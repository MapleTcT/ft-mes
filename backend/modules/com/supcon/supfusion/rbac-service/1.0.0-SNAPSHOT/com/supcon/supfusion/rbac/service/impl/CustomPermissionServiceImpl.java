package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.CustomPermissionMapper;
import com.supcon.supfusion.rbac.dao.po.CustomPermissionPO;
import com.supcon.supfusion.rbac.service.ICustomPermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 自定义权限表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Slf4j
@Service
@Transactional
public class CustomPermissionServiceImpl extends ServiceImpl<CustomPermissionMapper, CustomPermissionPO> implements ICustomPermissionService {
}
