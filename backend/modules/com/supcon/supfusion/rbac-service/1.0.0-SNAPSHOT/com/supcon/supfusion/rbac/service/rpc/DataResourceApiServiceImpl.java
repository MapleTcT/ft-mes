package com.supcon.supfusion.rbac.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.rbac.api.DataResourceApiService;
import com.supcon.supfusion.rbac.api.dto.DataResouceDTO;
import com.supcon.supfusion.rbac.api.dto.UserDataResourceDTO;
import com.supcon.supfusion.rbac.dao.RoleUserMapper;
import com.supcon.supfusion.rbac.dao.po.RbacRoleDataPermissionCtrlPO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionCtrlPO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionPO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.bo.UserDataResourceResponseBO;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacRoleDataPermissionCtrlServiceImpl;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacUserDataPermissionCtrlServiceImpl;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacUserDataPermissionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ServiceApiService
@Slf4j
public class DataResourceApiServiceImpl implements DataResourceApiService {
    @Autowired
    private RbacUserDataPermissionServiceImpl rbacUserDataResourcePermissionService;
    @Autowired
    private RbacUserDataPermissionCtrlServiceImpl rbacUserDataResourcePermissionControlledService;
    @Autowired
    private RbacRoleDataPermissionCtrlServiceImpl rbacRoleDataPermissionCtrlService;
    @Autowired
    private RoleUserMapper roleUserMapper;

    @Override
    public UserDataResourceDTO queryDataResourceByUser(Long userId, String resServiceCode, Long cid, String resKey, String resType) {
        UserDataResourceDTO userDataResourceDTO = new UserDataResourceDTO();

        RbacUserDataPermissionCtrlPO rbacUserDataPermissionCtrlPO = rbacUserDataResourcePermissionControlledService.getOne(Wrappers.<RbacUserDataPermissionCtrlPO>query()
                .eq(RbacUserDataPermissionCtrlPO.getUserIdFieldName(), userId)
                .eq(RbacUserDataPermissionCtrlPO.getCidFieldName(), cid)
                .eq(RbacUserDataPermissionCtrlPO.getGroupCodeFieldName(), resServiceCode));
        if (rbacUserDataPermissionCtrlPO != null && rbacUserDataPermissionCtrlPO.getControlled() == 0) {
            userDataResourceDTO.setControlled(false);
            return userDataResourceDTO;
        }
        List<RoleUserPO> roleUserPOS = roleUserMapper.selectList(Wrappers.<RoleUserPO>query().eq(RoleUserPO.getUserIdFeildName(), userId));
        if (roleUserPOS != null && roleUserPOS.size() > 0) {
            /**
             * 该用户在当前公司下如果有绑定的角色权限不受控制，那么该用户的权限就不受控制
             */
            List<Long> roleIds = roleUserPOS.stream().map(roleUserPO -> roleUserPO.getRoleId()).collect(Collectors.toList());
            int count = rbacRoleDataPermissionCtrlService.count(Wrappers.<RbacRoleDataPermissionCtrlPO>query()
                    .eq(RbacRoleDataPermissionCtrlPO.getCidFieldName(), cid)
                    .eq(RbacRoleDataPermissionCtrlPO.getControlledFieldName(), 0)
                    .in(RbacRoleDataPermissionCtrlPO.getRoleIdFieldName(), roleIds));
            if (count > 0) {
                userDataResourceDTO.setControlled(false);
                return userDataResourceDTO;
            }
        }


        userDataResourceDTO.setControlled(true);
        QueryWrapper queryWrapper = Wrappers.<RbacUserDataPermissionPO>query()
                .eq(RbacUserDataPermissionPO.getUserIdFieldName(), userId)
                .eq(RbacUserDataPermissionPO.getCidFieldName(), cid)
                .eq(RbacUserDataPermissionPO.getGroupCodeFieldName(), resServiceCode);
        if (!StringUtils.isBlank(resKey)) {
            queryWrapper.eq(RbacUserDataPermissionPO.getResourceCodeFieldName(), resKey);
        }
        if (!StringUtils.isBlank(resType)) {
            queryWrapper.eq(RbacUserDataPermissionPO.getResourceTypeFieldName(), resType);
        }
        List<RbacUserDataPermissionPO> rbacUserDataPermissionPOS = rbacUserDataResourcePermissionService.list(queryWrapper);
        List<DataResouceDTO> dataResouces = new ArrayList<>();
        userDataResourceDTO.setDataResouces(dataResouces);
        if (rbacUserDataPermissionPOS != null) {
            List<String> existResourceCode = new ArrayList<>();
            for (RbacUserDataPermissionPO rbacUserDataPermissionPO : rbacUserDataPermissionPOS) {
                //根据resourceCode去重
                if (existResourceCode.contains(rbacUserDataPermissionPO.getResourceCode())) {
                    continue;
                } else {
                    existResourceCode.add(rbacUserDataPermissionPO.getResourceCode());
                }
                DataResouceDTO dataResouceDTO = new DataResouceDTO();
                dataResouces.add(dataResouceDTO);
                dataResouceDTO.setResourceCode(rbacUserDataPermissionPO.getResourceCode());
                dataResouceDTO.setResourceName(rbacUserDataPermissionPO.getResourceName());
                dataResouceDTO.setResourceType(rbacUserDataPermissionPO.getResourceType());
            }
        }

        return userDataResourceDTO;
    }
}
