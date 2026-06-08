package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.FlowPermissionPositionMapper;
import com.supcon.supfusion.rbac.dao.po.FlowPermissionPositionPO;
import com.supcon.supfusion.rbac.service.IFlowPermissionPositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class FlowPermissionPositionServiceImpl extends ServiceImpl<FlowPermissionPositionMapper, FlowPermissionPositionPO> implements IFlowPermissionPositionService {
}
