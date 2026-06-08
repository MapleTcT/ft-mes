package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.FlowPermissionStaffMapper;
import com.supcon.supfusion.rbac.dao.po.FlowPermissionStaffPO;
import com.supcon.supfusion.rbac.service.IFlowPermissionStaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class FlowPermissionStaffServiceImpl extends ServiceImpl<FlowPermissionStaffMapper, FlowPermissionStaffPO> implements IFlowPermissionStaffService {
}
