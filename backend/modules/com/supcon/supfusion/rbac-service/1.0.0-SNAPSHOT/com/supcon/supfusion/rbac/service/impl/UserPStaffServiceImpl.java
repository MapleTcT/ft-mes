package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.UserPStaffMapper;
import com.supcon.supfusion.rbac.dao.po.UserPStaffPO;
import com.supcon.supfusion.rbac.service.IUserPStaffService;
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
public class UserPStaffServiceImpl extends ServiceImpl<UserPStaffMapper, UserPStaffPO> implements IUserPStaffService {

}
