package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.RolePDepartmentMapper;
import com.supcon.supfusion.rbac.dao.UserPDepartmentMapper;
import com.supcon.supfusion.rbac.dao.po.RolePDepartmentPO;
import com.supcon.supfusion.rbac.dao.po.UserPDepartmentPO;
import com.supcon.supfusion.rbac.service.IRolePDepartmentService;
import com.supcon.supfusion.rbac.service.IUserPDepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Slf4j
@Service
@Transactional
public class RolePDepartmentServiceImpl extends ServiceImpl<RolePDepartmentMapper, RolePDepartmentPO> implements IRolePDepartmentService {
}
