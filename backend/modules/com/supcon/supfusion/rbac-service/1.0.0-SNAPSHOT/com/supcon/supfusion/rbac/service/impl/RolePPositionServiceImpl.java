package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.RolePPositionMapper;
import com.supcon.supfusion.rbac.dao.po.RolePPositionPO;
import com.supcon.supfusion.rbac.service.IRolePPositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 角色指定岗位表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Slf4j
@Service
@Transactional
public class RolePPositionServiceImpl extends ServiceImpl<RolePPositionMapper, RolePPositionPO> implements IRolePPositionService {

}
