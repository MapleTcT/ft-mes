package com.supcon.supfusion.notification.admin.manager.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.notification.admin.manager.service.RbacService;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RbacServiceImpl implements RbacService {

    @ServiceApiReference
    private IRoleApiService iRbacApiService;

    @Override
    public List<RoleDTO> getRoles(List<String> codes) {
        log.info("rbac.findUserByRoleCode param :{}", codes != null ? JSONArray.toJSONString(codes) : null);
        List<RoleDTO> roleDTOS = iRbacApiService.findRoleByCodes(codes);
        log.info("rbac.findUserByRoleCode return :{}", roleDTOS != null ? JSONArray.toJSONString(roleDTOS) : null);
        return roleDTOS;
    }
}
