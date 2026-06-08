package com.supcon.supfusion.rbac.service.rpc;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.rbac.api.IPermissionApiService;
import com.supcon.supfusion.rbac.api.dto.*;
import com.supcon.supfusion.rbac.api.enums.MethodType;
import com.supcon.supfusion.rbac.common.utils.ThreadPoolUtils;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@ServiceApiService
@Transactional
@Slf4j
public class PermissionApiServiceImpl implements IPermissionApiService {

    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private IFlowPermissionStaffService flowPermissionStaffService;
    @Autowired
    private IFlowPermissionPositionService flowPermissionPositionService;
    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private IRolePPositionService rolePPositionService;
    @Autowired
    private IRolePStaffService rolePStaffService;
    @Autowired
    private IRoleCustomPermissionRefService roleCustomPermissionRefService;
    @Autowired
    private IRoleDataPermissionService roleDataPermissionService;
    @Autowired
    private IDataPermissionRshowService dataPermissionRshowService;
    @Autowired
    private IUserPermissionService userPermissionService;
    @Autowired
    private ICustomPermissionService customPermissionService;
    @Autowired
    private IDataPermissionUshowService dataPermissionUshowService;
    @Autowired
    private IUserPPositionService userPPositionService;
    @Autowired
    private IUserDataPermissionService userDataPermissionService;
    @Autowired
    private IUserPStaffService userPStaffService;
    @Autowired
    private IUserCustomPermissionRefService userCustomPermissionRefService;
    @Autowired
    private IUserUrlRefService userUrlRefService;

    @Override
    public String generateBaseModelSql(Long userId, String menuOperateCode, Long currentCompanyId, String entityTable) {
        return flowPermissionService.generateBaseModelSql(userId,menuOperateCode,currentCompanyId,entityTable);
    }

    @Override
    public void deleteFlowPermission(String flowKey, List<String> activityCodes) {
        QueryWrapper<FlowPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FLOW_KEY",flowKey);
        queryWrapper.in(!ObjectUtils.isEmpty(activityCodes),"ACTIVITY_CODE",activityCodes);
        flowPermissionService.remove(queryWrapper);
    }

    @Override
    public void deleteFlowPermissionStaff(String flowKey, List<String> activityCodes) {
        QueryWrapper<FlowPermissionStaffPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("FLOWPERMISSION_ID in (select id from rbac_flow_permission where 1=1");
        queryWrapper.eq("FLOW_KEY",flowKey);
        queryWrapper.in(!ObjectUtils.isEmpty(activityCodes),"ACTIVITY_CODE",activityCodes);
        queryWrapper.last(")");
        flowPermissionStaffService.remove(queryWrapper);
    }

    @Override
    public void deleteFlowPermissionPosition(String flowKey, List<String> activityCodes) {
        QueryWrapper<FlowPermissionPositionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("FLOWPERMISSION_ID in (select id from rbac_flow_permission where 1=1");
        queryWrapper.eq("FLOW_KEY",flowKey);
        queryWrapper.in(!ObjectUtils.isEmpty(activityCodes),"ACTIVITY_CODE",activityCodes);
        queryWrapper.last(")");
        flowPermissionPositionService.remove(queryWrapper);
    }

    @Override
    public FlowPermissionDTO saveFlowPermission(FlowPermissionDTO flowPermissionDTO) {
        if (!ObjectUtils.isEmpty(flowPermissionDTO)){
            FlowPermissionPO flowPermissionPO = JSON.parseObject(JSON.toJSONString(flowPermissionDTO), FlowPermissionPO.class);
            flowPermissionService.saveOrUpdate(flowPermissionPO);
            return JSON.parseObject(JSON.toJSONString(flowPermissionPO), FlowPermissionDTO.class);
        }
        return null;
    }

    @Override
    public FlowPermissionPositionDTO saveFlowPermissionPosition(FlowPermissionPositionDTO flowPermissionPositionDTO) {
        if (!ObjectUtils.isEmpty(flowPermissionPositionDTO)){
            FlowPermissionPositionPO flowPermissionPositionPO = new FlowPermissionPositionPO();
            BeanUtils.copyProperties(flowPermissionPositionDTO,flowPermissionPositionPO);
            flowPermissionPositionService.saveOrUpdate(flowPermissionPositionPO);
            FlowPermissionPositionDTO flowPermissionPositionDTO1 = new FlowPermissionPositionDTO();
            BeanUtils.copyProperties(flowPermissionPositionPO,flowPermissionPositionDTO1);
            return flowPermissionPositionDTO1;
        }
        return null;
    }

    @Override
    public FlowPermissionStaffDTO saveFlowPermissionStaff(FlowPermissionStaffDTO flowPermissionStaffDTO) {
        if (!ObjectUtils.isEmpty(flowPermissionStaffDTO)){
            FlowPermissionStaffPO flowPermissionStaffPO = new FlowPermissionStaffPO();
            BeanUtils.copyProperties(flowPermissionStaffDTO,flowPermissionStaffPO);
            flowPermissionStaffService.saveOrUpdate(flowPermissionStaffPO);
            FlowPermissionStaffDTO flowPermissionStaffDTO1 = new FlowPermissionStaffDTO();
            BeanUtils.copyProperties(flowPermissionStaffPO,flowPermissionStaffDTO1);
            return flowPermissionStaffDTO1;
        }
        return null;
    }

    @Override
    public void deleteFlowPermissionById(List<Long> ids) {
        flowPermissionService.removeByIds(ids);
    }

    @Override
    public void deleteFlowPermissionPositionById(List<Long> ids) {
        flowPermissionPositionService.removeByIds(ids);
    }

    @Override
    public void deleteFlowPermissionStaffById(List<Long> ids) {
        flowPermissionStaffService.removeByIds(ids);
    }

    @Override
    public void refreshRedis(List<String> apps) {
        if (ObjectUtils.isEmpty(apps)){
            return;
        }
        ThreadPoolUtils.getThreadPool().execute(() -> {
            userUrlRefService.refreshRedis(apps);
        });

    }


    @Override
    public Map<String, List<String>> refreshRedisByUser(Long userId, Long cid, String tenantId, String method) {
        Map<String, Map<String, List<String>>> result = userUrlRefService.addUserUrlRefListForUserFlow(Collections.singletonList(userId), cid, tenantId);
        if (!ObjectUtils.isEmpty(result)){
            return result.get(userId + method);
        }else{
            return new HashMap<>();
        }
    }

}
