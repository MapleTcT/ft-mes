package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.UserPPositionMapper;
import com.supcon.supfusion.rbac.dao.po.UserPPositionPO;
import com.supcon.supfusion.rbac.service.IUserPPositionService;
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
public class UserPPositionServiceImpl extends ServiceImpl<UserPPositionMapper, UserPPositionPO> implements IUserPPositionService {

}
