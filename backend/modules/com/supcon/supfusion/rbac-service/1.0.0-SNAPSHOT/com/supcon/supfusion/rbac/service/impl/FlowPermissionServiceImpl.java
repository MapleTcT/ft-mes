package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.rbac.dao.*;
import com.supcon.supfusion.rbac.dao.bo.FlowPermissionBO;
import com.supcon.supfusion.rbac.dao.enums.FlowPermissionType;
import com.supcon.supfusion.rbac.dao.enums.MenuOperateType;
import com.supcon.supfusion.rbac.dao.field.FlowPermissionField;
import com.supcon.supfusion.rbac.dao.field.FlowPermissionPositionField;
import com.supcon.supfusion.rbac.dao.field.FlowPermissionStaffField;
import com.supcon.supfusion.rbac.dao.field.MenuOperateField;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.dao.query.FlowPermissionQuery;
import com.supcon.supfusion.rbac.manager.IOrganizationAdapter;
import com.supcon.supfusion.rbac.manager.IUserAdapter;
import com.supcon.supfusion.rbac.service.IFlowPermissionPositionService;
import com.supcon.supfusion.rbac.service.IFlowPermissionService;
import com.supcon.supfusion.rbac.service.IFlowPermissionStaffService;
import com.supcon.supfusion.rbac.service.IMenuOperateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 工作流数据权限表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Slf4j
@Service
@Transactional
public class FlowPermissionServiceImpl extends ServiceImpl<FlowPermissionMapper, FlowPermissionPO> implements IFlowPermissionService {

    @Autowired
    private UserPermissionMapper userPermissionMapper;
    @Autowired
    private UserPPositionMapper userPPositionMapper;
    @Autowired
    private UserPDepartmentMapper userPDepartmentMapper;
    @Autowired
    private UserPStaffMapper userPStaffMapper;
    @Autowired
    private IUserAdapter userAdapter;
    @Autowired
    private IOrganizationAdapter organizationAdapter;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IFlowPermissionPositionService flowPermissionPositionService;
    @Autowired
    private IFlowPermissionStaffService flowPermissionStaffService;
    @Autowired
    private FlowPermissionMapper flowPermissionMapper;

    /**
     * @description: 查询 岗位、用户、部门限制 返回各ID
     * @param: userId
     * @param: menuOperateCode
     * @param: currentCompanyId
     * @return: java.util.Map<java.lang.String,java.util.List<java.lang.Long>>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public Map<String, List<Long>> getPendingPowerCondition(Long userId, String menuOperateCode, Long currentCompanyId) {
        List<Map<String, Object>> checkMap = userPermissionMapper.findUserPermissionValidMenuOperate(menuOperateCode, userId);
        Map<String, List<Long>> resultMap = new HashMap<>();
        resultMap.put("positions", new ArrayList<>());
        resultMap.put("users", new ArrayList<>());
        resultMap.put("departments", new ArrayList<>());
        // 岗位限制
        boolean positonFlag = false;
        // 无限制
        boolean noRestrictFlag = false;
        long userpermissionId = -1L;
        // 指定岗位
        boolean assignPositonFlag = false;
        // 指定部门
        boolean assignDepartmentFlag = false;
        // 部门限制
        boolean departmentFlag = false;
        // 指定人员
        boolean assignStaffFlag = false;
        if (null != checkMap && checkMap.size() > 0) {
            for (Map<String, Object> objectMap : checkMap) {
                positonFlag = objectMap.get("POSITIONFLAG") != null && ((Number) objectMap.get("POSITIONFLAG")).intValue() == 1;
                departmentFlag = objectMap.get("DEPARTMENTFLAG") != null && ((Number) objectMap.get("DEPARTMENTFLAG")).intValue() == 1;
                userpermissionId = (Long.decode(objectMap.get("ID").toString()));
                noRestrictFlag = objectMap.get("NORESTRICTFLAG") != null && ((Number) objectMap.get("NORESTRICTFLAG")).intValue() == 1;
                assignPositonFlag = objectMap.get("ASSIGNPOSFLAG") != null && ((Number) objectMap.get("ASSIGNPOSFLAG")).intValue() == 1;
                assignStaffFlag = objectMap.get("ASSIGNSTAFFFLAG") != null && ((Number) objectMap.get("ASSIGNSTAFFFLAG")).intValue() == 1;
                assignDepartmentFlag = objectMap.get("ASSIGNDEPTFLAG") != null && ((Number) objectMap.get("ASSIGNDEPTFLAG")).intValue() == 1;
            }
        }
        // 无限制直接返回
        if (noRestrictFlag) {
            return resultMap;
        }
        // 如果是岗位限制
        if (positonFlag) {
            //现在网关还无法获取当前登陆人的岗位ID，后续网关会补上 2020/7/21 yy
            if (!ObjectUtils.isEmpty(UserContext.getUserContext().getPositionId())){
                List<Long> positionIdsByPositionId = organizationAdapter.querySubPositionIdsByPositionId(Collections.singletonList(UserContext.getUserContext().getPositionId()));
                resultMap.get("positions").addAll(positionIdsByPositionId);
            }
        }

        // 指定岗位限制
        if (assignPositonFlag) {
            List<UserPPositionPO> pPositionPOS = userPPositionMapper.selectList(new QueryWrapper<UserPPositionPO>().eq("USERPERMISSION_ID", userpermissionId));
            if (pPositionPOS != null && pPositionPOS.size() > 0) {
                List<Long> posIds = new ArrayList<>();
                List<Long> includeLowerIds = new ArrayList<>();
                for (UserPPositionPO pPositionPO : pPositionPOS) {
                    //如果需要查下级岗位
                    if (pPositionPO.getIncludeLower()) {
                        includeLowerIds.add(pPositionPO.getPositionId());
                    } else {
                        posIds.add(pPositionPO.getPositionId());
                    }
                }
                List<Long> querySubPositionIdsByPositionIds = new ArrayList<>();
                if (!ObjectUtils.isEmpty(includeLowerIds)){
                    querySubPositionIdsByPositionIds = organizationAdapter.querySubPositionIdsByPositionId(includeLowerIds);
                }
                posIds.addAll(querySubPositionIdsByPositionIds);
                resultMap.get("positions").addAll(posIds);
            }
        }

        // 指定人员限制
        if (assignStaffFlag) {
            List<UserPStaffPO> pStaffPOS = userPStaffMapper.selectList(new QueryWrapper<UserPStaffPO>().eq("USERPERMISSION_ID", userpermissionId));
            resultMap.put("users", new ArrayList<>());
            List<Long> staffIds = new ArrayList<>();
            if (null != pStaffPOS && pStaffPOS.size() > 0) {
                for (UserPStaffPO staff : pStaffPOS) {
                    staffIds.add(staff.getStaffId());
                }
                Map<Long, UserDetailDTO> idMap = userAdapter.getUserIdByPersonId(staffIds.stream().map(Object::toString).collect(Collectors.joining(",")));
                idMap.forEach((id, user) -> {
                    resultMap.get("users").add(user.getPersonId());
                });
            }
        }
        //加上userid是本身的代办
        resultMap.get("users").add(UserContext.getUserContext().getStaffId());
        // 如果是部门限制
        if (departmentFlag) {
            //现在网关还无法获取当前登陆人的部门ID，后续网关会补上 2020/7/21 yy
            if (!ObjectUtils.isEmpty(UserContext.getUserContext().getDepartmentId())){
                List<Long> querySubDepartmentIdsByDepartmentId = organizationAdapter.querySubDepartmentIdsByDepartmentId(Collections.singletonList(UserContext.getUserContext().getDepartmentId()));
                resultMap.get("departments").addAll(querySubDepartmentIdsByDepartmentId);
            }
        }
        if (assignDepartmentFlag) {
            List<UserPDepartmentPO> userPDepartmentPOS = userPDepartmentMapper.selectList(new QueryWrapper<UserPDepartmentPO>().eq("USERPERMISSION_ID", userpermissionId));
            if (userPDepartmentPOS != null && userPDepartmentPOS.size() > 0) {
                List<Long> depIds = new ArrayList<>();
                List<Long> includeLowerIds = new ArrayList<>();
                for (UserPDepartmentPO pDepartmentPO : userPDepartmentPOS) {
                    //如果需要查下级岗位
                    if (pDepartmentPO.getIncludeLower()) {
                        includeLowerIds.add(pDepartmentPO.getDepartmentId());
                    } else {
                        depIds.add(pDepartmentPO.getDepartmentId());
                    }
                }
                List<Long> querySubDepartmentIdsByDepartmentId = new ArrayList<>();
                if (!ObjectUtils.isEmpty(includeLowerIds)){
                    querySubDepartmentIdsByDepartmentId = organizationAdapter.querySubDepartmentIdsByDepartmentId(includeLowerIds);
                }
                depIds.addAll(querySubDepartmentIdsByDepartmentId);
                resultMap.get("departments").addAll(depIds);
            }
        }
        return resultMap;
    }

    /**
     * @description: 拼接数据权限sql
     * @param: userId
     * @param: menuOperateCode
     * @param: currentCompanyId
     * @param: entityTable
     * @return: java.lang.String
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public String generateBaseModelSql(Long userId, String menuOperateCode, Long currentCompanyId, String entityTable) {
        String ownerPositionId = "OWNER_POSITION_ID";
        String ownerStaffId = "OWNER_STAFF_ID";
        String ownerDepartmentId = "OWNER_DEPARTMENT_ID";
        Map<String, List<Long>> condition = getPendingPowerCondition(userId, menuOperateCode, currentCompanyId);
        StringBuilder sb = new StringBuilder();
        List<Long> positions = condition.get("positions");
        positions = positions.stream().filter(id -> !ObjectUtils.isEmpty(id)).collect(Collectors.toList());
        List<Long> departments = condition.get("departments");
        departments = departments.stream().filter(id -> !ObjectUtils.isEmpty(id)).collect(Collectors.toList());
        List<Long> users = condition.get("users");
        users = users.stream().filter(id -> !ObjectUtils.isEmpty(id)).collect(Collectors.toList());
        //处理岗位
        if (!ObjectUtils.isEmpty(positions)){
            //大于1000个的话需要批次处理
            int batch = positions.size() / 1000;
            for (int i = 0; i < batch; i++) {
                List<Long> subList = positions.subList(i * 1000, i * 1000 + 1000);
                sb.append(" OR ")
                        .append(entityTable)
                        .append(".")
                        .append(ownerPositionId)
                        .append(" IN (")
                        .append(subList.stream().map(Object::toString).collect(Collectors.joining(",")))
                        .append(")");
            }
            List<Long> subList = positions.subList(batch * 1000, positions.size());
            sb.append(" OR ")
                    .append(entityTable)
                    .append(".")
                    .append(ownerPositionId)
                    .append(" IN (")
                    .append(subList.stream().map(Object::toString).collect(Collectors.joining(",")))
                    .append(")");
        }

        //处理人员
        if (!ObjectUtils.isEmpty(users)){
            //大于1000个的话需要批次处理
            int batch = users.size() / 1000;
            for (int i = 0; i < batch; i++) {
                List<Long> subList = users.subList(i * 1000, i * 1000 + 1000);
                sb.append(" OR ")
                        .append(entityTable)
                        .append(".")
                        .append(ownerStaffId)
                        .append(" IN (")
                        .append(subList.stream().map(Object::toString).collect(Collectors.joining(",")))
                        .append(")");
            }
            List<Long> subList = users.subList(batch * 1000, users.size());
            sb.append(" OR ")
                    .append(entityTable)
                    .append(".")
                    .append(ownerStaffId)
                    .append(" IN (")
                    .append(subList.stream().map(Object::toString).collect(Collectors.joining(",")))
                    .append(")");
        }

        //处理部门
        if (!ObjectUtils.isEmpty(departments)){
            //大于1000个的话需要批次处理
            int batch = users.size() / 1000;
            for (int i = 0; i < batch; i++) {
                List<Long> subList = departments.subList(i * 1000, i * 1000 + 1000);
                sb.append(" OR ")
                        .append(entityTable)
                        .append(".")
                        .append(ownerDepartmentId)
                        .append(" IN (")
                        .append(subList.stream().map(Object::toString).collect(Collectors.joining(",")))
                        .append(")");
            }
            List<Long> subList = departments.subList(batch * 1000, departments.size());
            sb.append(" OR ")
                    .append(entityTable)
                    .append(".")
                    .append(ownerDepartmentId)
                    .append(" IN (")
                    .append(subList.stream().map(Object::toString).collect(Collectors.joining(",")))
                    .append(")");
        }

        return sb.toString();

    }

    @Override
    @Transactional
    public void deleteFlowPermissionByUserPermission(List<UserPermissionPO> upList) {
        if (ObjectUtils.isEmpty(upList)){
            return;
        }
        Map<Long, List<UserPermissionPO>> groupUserId = upList.stream().collect(Collectors.groupingBy(UserPermissionPO::getUserId));
        Set<Long> userIds = groupUserId.keySet();
        List<Long> moIds = upList.stream().map(UserPermissionPO::getMenuOperateId).collect(Collectors.toList());
        // sql 超长处理
        QueryWrapper<MenuOperatePO> operatePOQueryWrapper = new QueryWrapper<>();
        int batch = moIds.size() / 1000;
        if (0 == batch) {
            operatePOQueryWrapper.in(MenuOperateField.id, moIds);
        } else {
            for (int i = 0; i < batch; i++) {
                operatePOQueryWrapper.or().in(MenuOperateField.id, moIds.subList(i * 1000, i * 1000 + 1000));
            }
            if (moIds.size() % 1000 != 0) {
                operatePOQueryWrapper.or().in(MenuOperateField.id, moIds.subList(batch * 1000, moIds.size()));
            }
        }
        List<MenuOperatePO> operatePOS = menuOperateService.list(operatePOQueryWrapper);


        //挑选工作流相关操作 通过menuOperateType判断
        List<MenuOperatePO> flow_operates = operatePOS.stream().filter(menuOperatePO -> !ObjectUtils.isEmpty(menuOperatePO.getFlowKey())).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(flow_operates)){
            return;
        }
        //查询该操作，该人员的工作流权限
        //需要区分流程版本去删除
        List<String> operate_codes = flow_operates.stream().map(MenuOperatePO::getCode).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(operate_codes)){
            return;
        }
        // sql 超长处理
        QueryWrapper<FlowPermissionPO> queryWrapper = new QueryWrapper<>();
        int codeBatch = operate_codes.size() / 1000;
        if (0 == codeBatch) {
            queryWrapper.in(FlowPermissionField.activityCode, operate_codes);
        } else {
            for (int i = 0; i < codeBatch; i++) {
                queryWrapper.or().in(FlowPermissionField.activityCode, operate_codes.subList(i * 1000, i * 1000 + 1000));
            }
            if (operate_codes.size() % 1000 != 0) {
                queryWrapper.or().in(FlowPermissionField.activityCode, operate_codes.subList(codeBatch * 1000, operate_codes.size()));
            }
        }
//        queryWrapper.in(FlowPermissionField.activityCode,operate_codes);
        queryWrapper.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.USER);
        queryWrapper.in(FlowPermissionField.typeId,userIds);
        queryWrapper.select(FlowPermissionField.id);
        List<FlowPermissionPO> flowPermissionPOS = this.list(queryWrapper);
        if (ObjectUtils.isEmpty(flowPermissionPOS)){
            return;
        }
        List<Long> flow_permission_ids = flowPermissionPOS.stream().map(FlowPermissionPO::getId).collect(Collectors.toList());
        this.removeByIds(flow_permission_ids);
        //删除关联岗位
        flowPermissionPositionService.remove(new QueryWrapper<FlowPermissionPositionPO>().in(FlowPermissionPositionField.flowpermissionId,flow_permission_ids));
        //删除关联人员
        flowPermissionStaffService.remove(new QueryWrapper<FlowPermissionStaffPO>().in(FlowPermissionStaffField.flowpermissionId,flow_permission_ids));
    }

    @Override
    public void insertFlowPermissionByUserPermission(List<UserPermissionPO> upList,Map<Long, MenuOperatePO> operateMap) {
        if (ObjectUtils.isEmpty(upList)){
            return;
        }
        Map<Long, List<UserPermissionPO>> groupUserId = upList.stream().collect(Collectors.groupingBy(UserPermissionPO::getUserId));
        //查询已存在的工作流权限
        List<FlowPermissionPO> flow_list = new ArrayList<>();
        groupUserId.forEach((userId,permissions) ->{
            List<String> operate_codes = permissions.stream().map(userPermissionPO -> {
                MenuOperatePO operatePO = operateMap.get(userPermissionPO.getMenuOperateId());
                return operatePO.getCode();
            }).collect(Collectors.toList());
            // sql 超长处理
            QueryWrapper<FlowPermissionPO> flowPermissionWrapper = new QueryWrapper<>();
            int batch = operate_codes.size() / 1000;
            if (0 == batch) {
                flowPermissionWrapper.in(FlowPermissionField.activityCode, operate_codes);
            } else {
                for (int i = 0; i < batch; i++) {
                    flowPermissionWrapper.or().in(FlowPermissionField.activityCode, operate_codes.subList(i * 1000, i * 1000 + 1000));
                }
                if (operate_codes.size() % 1000 != 0) {
                    flowPermissionWrapper.or().in(FlowPermissionField.activityCode, operate_codes.subList(batch * 1000, operate_codes.size()));
                }
            }
            List<FlowPermissionPO> list = this.list(flowPermissionWrapper.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.USER).eq(FlowPermissionField.typeId, userId));
            if (!ObjectUtils.isEmpty(list)) {
                flow_list.addAll(list);
            }
        });
        //人员关联
        List<FlowPermissionStaffPO> staffPOList = new ArrayList<>();
        //岗位关联
        List<FlowPermissionPositionPO> positionPOList = new ArrayList<>();
        List<FlowPermissionPO> flowPermissionPOS = upList.stream().map(userPermissionPO -> {
            MenuOperatePO operatePO = operateMap.get(userPermissionPO.getMenuOperateId());
            if (ObjectUtils.isEmpty(operatePO.getFlowKey())){
                return null;
            }
            FlowPermissionPO flowPermissionPO = new FlowPermissionPO();
            BeanUtils.copyProperties(userPermissionPO,flowPermissionPO);
            flowPermissionPO.setEntityCode(operatePO.getEntityCode());
            flowPermissionPO.setActivityCode(operatePO.getCode());
            if (operatePO.getMenuOperateType().equals(MenuOperateType.ACTIVEOPERATE.name())) {
                flowPermissionPO.setPurviewState(2);
            }else{
                flowPermissionPO.setPurviewState(1);
            }
            flowPermissionPO.setFlowKey(operatePO.getFlowKey());
            flowPermissionPO.setFlowVersion(operatePO.getFlowVersion());
            flowPermissionPO.setTypeId(userPermissionPO.getUserId());
            flowPermissionPO.setFlowPermissionType(FlowPermissionType.USER);
            flowPermissionPO.setPurviewDistribution(1);
            flowPermissionPO.setUnlimitedPower(userPermissionPO.getNoRestrictFlag());
            flowPermissionPO.setPositionPowerFlag(userPermissionPO.getPositionFlag());
            flowPermissionPO.setGroupPowerFlag(!ObjectUtils.isEmpty(userPermissionPO.getGroupFlag()) && userPermissionPO.getGroupFlag() == 1);
            Optional<FlowPermissionPO> exist_flow = flow_list.stream().filter(flowPermission -> flowPermissionPO.getTypeId().equals(flowPermission.getTypeId()) && flowPermissionPO.getActivityCode().equals(flowPermission.getActivityCode())
                    && flowPermissionPO.getFlowKey().equals(flowPermission.getFlowKey()) && flowPermissionPO.getFlowVersion().equals(flowPermission.getFlowVersion())).findFirst();
            if (exist_flow.isPresent()){
                flowPermissionPO.setId(exist_flow.get().getId());
            }else{
                flowPermissionPO.setId(IDGenerator.newInstance().generate().longValue());
            }
            if (!ObjectUtils.isEmpty(userPermissionPO.getAssignStaffFlag()) && userPermissionPO.getAssignStaffFlag()){
                staffPOList.addAll(this.generateStaffRefUser(userPermissionPO,flowPermissionPO));
            }
            if (!ObjectUtils.isEmpty(userPermissionPO.getAssignPosFlag()) && userPermissionPO.getAssignPosFlag()){
                positionPOList.addAll(this.generatePositionRefUser(userPermissionPO,flowPermissionPO));
            }
            return flowPermissionPO;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        this.saveFlowPermission(flowPermissionPOS,staffPOList,positionPOList);
    }

    @Override
    public void deleteFlowPermissionByRolePermission(List<RolePermissionPO> rpList) {
        if (ObjectUtils.isEmpty(rpList)){
            return;
        }
        Map<Long, List<RolePermissionPO>> groupRoleId = rpList.stream().collect(Collectors.groupingBy(RolePermissionPO::getRoleId));
        Set<Long> roleIds = groupRoleId.keySet();
        List<Long> moIds = rpList.stream().map(RolePermissionPO::getMenuOperateId).collect(Collectors.toList());
        List<MenuOperatePO> operatePOS = menuOperateService.listByIds(moIds);
        //挑选工作流相关操作 通过menuOperateType判断
        List<MenuOperatePO> flow_operates = operatePOS.stream().filter(menuOperatePO -> !ObjectUtils.isEmpty(menuOperatePO.getFlowKey())).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(flow_operates)){
            return;
        }
        //查询该操作，该人员的工作流权限
        //需要区分流程版本去删除
        Map<String, List<MenuOperatePO>> flow_operate_version = flow_operates.stream().collect(Collectors.groupingBy(MenuOperatePO::getFlowVersion));
        flow_operate_version.forEach((version,list) -> {
            List<String> operate_codes = list.stream().map(MenuOperatePO::getCode).collect(Collectors.toList());
            QueryWrapper<FlowPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.in(FlowPermissionField.activityCode,operate_codes);
            queryWrapper.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE);
            queryWrapper.in(FlowPermissionField.typeId,roleIds);
            queryWrapper.select(FlowPermissionField.id);
            List<FlowPermissionPO> flowPermissionPOS = this.list(queryWrapper);
            if (ObjectUtils.isEmpty(flowPermissionPOS)){
                return;
            }
            List<Long> flow_permission_ids = flowPermissionPOS.stream().map(FlowPermissionPO::getId).collect(Collectors.toList());
            this.removeByIds(flow_permission_ids);
            //删除关联岗位
            flowPermissionPositionService.remove(new QueryWrapper<FlowPermissionPositionPO>().in(FlowPermissionPositionField.flowpermissionId,flow_permission_ids));
            //删除关联人员
            flowPermissionStaffService.remove(new QueryWrapper<FlowPermissionStaffPO>().in(FlowPermissionStaffField.flowpermissionId,flow_permission_ids));
        });
    }

    @Override
    public void insertFlowPermissionByRolePermission(List<RolePermissionPO> rpList, Map<Long, MenuOperatePO> operateMap) {
        if (ObjectUtils.isEmpty(rpList)){
            return;
        }
        Map<Long, List<RolePermissionPO>> groupUserId = rpList.stream().collect(Collectors.groupingBy(RolePermissionPO::getRoleId));
        //查询已存在的工作流权限
        List<FlowPermissionPO> flow_list = new ArrayList<>();
        groupUserId.forEach((roleId,permissions) ->{
            List<String> operate_codes = permissions.stream().map(rolePermissionPO -> {
                MenuOperatePO operatePO = operateMap.get(rolePermissionPO.getMenuOperateId());
                return operatePO.getCode();
            }).collect(Collectors.toList());
            // sql 超长处理
            QueryWrapper<FlowPermissionPO> flowPermissionPOQueryWrapper = new QueryWrapper<>();
            int batch = operate_codes.size() / 1000;
            if (0 == batch) {
                flowPermissionPOQueryWrapper.in(FlowPermissionField.activityCode, operate_codes);
            } else {
                for (int i = 0; i < batch; i++) {
                    flowPermissionPOQueryWrapper.or().in(FlowPermissionField.activityCode, operate_codes.subList(i * 1000, i * 1000 + 1000));
                }
                if (operate_codes.size() % 1000 != 0) {
                    flowPermissionPOQueryWrapper.or().in(FlowPermissionField.activityCode, operate_codes.subList(batch * 1000, operate_codes.size()));
                }
            }
            List<FlowPermissionPO> list = this.list(flowPermissionPOQueryWrapper.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE).eq(FlowPermissionField.typeId, roleId));
            if (!ObjectUtils.isEmpty(list)){
                flow_list.addAll(list);
            }
        });
        //人员关联
        List<FlowPermissionStaffPO> staffPOList = new ArrayList<>();
        //岗位关联
        List<FlowPermissionPositionPO> positionPOList = new ArrayList<>();
        List<FlowPermissionPO> flowPermissionPOS = rpList.stream().map(rolePermissionPO -> {
            MenuOperatePO operatePO = operateMap.get(rolePermissionPO.getMenuOperateId());
            if (ObjectUtils.isEmpty(operatePO.getFlowKey())){
                return null;
            }
            FlowPermissionPO flowPermissionPO = new FlowPermissionPO();
            BeanUtils.copyProperties(rolePermissionPO,flowPermissionPO);
            flowPermissionPO.setEntityCode(operatePO.getEntityCode());
            flowPermissionPO.setActivityCode(operatePO.getCode());
            if (operatePO.getMenuOperateType().equals(MenuOperateType.ACTIVEOPERATE.name())) {
                flowPermissionPO.setPurviewState(2);
            }else{
                flowPermissionPO.setPurviewState(1);
            }
            flowPermissionPO.setFlowKey(operatePO.getFlowKey());
            flowPermissionPO.setFlowVersion(operatePO.getFlowVersion());
            flowPermissionPO.setTypeId(rolePermissionPO.getRoleId());
            flowPermissionPO.setFlowPermissionType(FlowPermissionType.ROLE);
            flowPermissionPO.setPurviewDistribution(0);
            flowPermissionPO.setUnlimitedPower(rolePermissionPO.getNoRestrictFlag());
            flowPermissionPO.setPositionPowerFlag(rolePermissionPO.getPositionFlag());
            flowPermissionPO.setGroupPowerFlag(!ObjectUtils.isEmpty(rolePermissionPO.getGroupFlag()) && rolePermissionPO.getGroupFlag() == 1);
            Optional<FlowPermissionPO> exist_flow = flow_list.stream().filter(flowPermission -> flowPermissionPO.getTypeId().equals(flowPermission.getTypeId()) && flowPermissionPO.getActivityCode().equals(flowPermission.getActivityCode())
                    && flowPermissionPO.getFlowKey().equals(flowPermission.getFlowKey()) && flowPermissionPO.getFlowVersion().equals(flowPermission.getFlowVersion())).findFirst();
            if (exist_flow.isPresent()){
                flowPermissionPO.setId(exist_flow.get().getId());
            }else{
                flowPermissionPO.setId(IDGenerator.newInstance().generate().longValue());
            }
            if (!ObjectUtils.isEmpty(rolePermissionPO.getAssignStaffFlag()) && rolePermissionPO.getAssignStaffFlag()){
                staffPOList.addAll(this.generateStaffRefRole(rolePermissionPO,flowPermissionPO));
            }
            if (!ObjectUtils.isEmpty(rolePermissionPO.getAssignPosFlag()) && rolePermissionPO.getAssignPosFlag()){
                positionPOList.addAll(this.generatePositionRefRole(rolePermissionPO,flowPermissionPO));
            }
            return flowPermissionPO;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        this.saveFlowPermission(flowPermissionPOS,staffPOList,positionPOList);
    }

    @Override
    public List<String> findUserFlowPermissionOperateCode(FlowPermissionQuery flowPermissionQuery) {
        return flowPermissionMapper.findUserFlowPermissionOperateCode(flowPermissionQuery);
    }

    @Override
    public List<FlowPermissionBO> findFlowPermissionByUserId(FlowPermissionQuery flowPermissionQuery) {
        return flowPermissionMapper.findFlowPermissionByUserId(flowPermissionQuery);
    }

    @Transactional
    private void saveFlowPermission(List<FlowPermissionPO> flowPermissionPOS,List<FlowPermissionStaffPO> flowPermissionStaffPOS,List<FlowPermissionPositionPO> flowPermissionPositionPOS){
        this.saveOrUpdateBatch(flowPermissionPOS);
        //删除关联数据
        List<Long> flow_ids = flowPermissionPOS.stream().map(FlowPermissionPO::getId).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(flow_ids)){
            return;
        }
        flowPermissionStaffService.remove(new QueryWrapper<FlowPermissionStaffPO>().in(FlowPermissionStaffField.flowpermissionId,flow_ids));
        flowPermissionPositionService.remove(new QueryWrapper<FlowPermissionPositionPO>().in(FlowPermissionPositionField.flowpermissionId,flow_ids));
        //新增关联数据
        flowPermissionStaffService.saveOrUpdateBatch(flowPermissionStaffPOS);
        flowPermissionPositionService.saveOrUpdateBatch(flowPermissionPositionPOS);
    }

    private List<FlowPermissionStaffPO> generateStaffRefUser(UserPermissionPO up, FlowPermissionPO fp){
        if (ObjectUtils.isEmpty(up) || ObjectUtils.isEmpty(up.getAssignStaffFlag()) || ObjectUtils.isEmpty(up.getStaffs())) {
            return new ArrayList<>();
        }
        return up.getStaffs().stream().map(userPStaffPO -> {
            FlowPermissionStaffPO flowPermissionStaffPO = new FlowPermissionStaffPO();
            flowPermissionStaffPO.setStaffId(userPStaffPO.getStaffId());
            flowPermissionStaffPO.setFlowpermissionId(fp.getId());
            return flowPermissionStaffPO;
        }).collect(Collectors.toList());
    }

    private List<FlowPermissionPositionPO> generatePositionRefUser(UserPermissionPO up, FlowPermissionPO fp){
        if (ObjectUtils.isEmpty(up) || ObjectUtils.isEmpty(up.getAssignPosFlag()) || ObjectUtils.isEmpty(up.getPositions())) {
            return new ArrayList<>();
        }
        return up.getPositions().stream().map(userPPositionPO -> {
            FlowPermissionPositionPO flowPermissionPositionPO = new FlowPermissionPositionPO();
            flowPermissionPositionPO.setPositionId(userPPositionPO.getPositionId());
            flowPermissionPositionPO.setFlowpermissionId(fp.getId());
            return flowPermissionPositionPO;
        }).collect(Collectors.toList());
    }

    private List<FlowPermissionStaffPO> generateStaffRefRole(RolePermissionPO rp,FlowPermissionPO fp){
        if (ObjectUtils.isEmpty(rp) || ObjectUtils.isEmpty(rp.getAssignStaffFlag()) || ObjectUtils.isEmpty(rp.getStaffs())) {
            return new ArrayList<>();
        }
        return rp.getStaffs().stream().map(rolePStaffPO -> {
            FlowPermissionStaffPO flowPermissionStaffPO = new FlowPermissionStaffPO();
            flowPermissionStaffPO.setStaffId(rolePStaffPO.getStaffId());
            flowPermissionStaffPO.setFlowpermissionId(fp.getId());
            return flowPermissionStaffPO;
        }).collect(Collectors.toList());
    }

    private List<FlowPermissionPositionPO> generatePositionRefRole(RolePermissionPO rp,FlowPermissionPO fp){
        if (ObjectUtils.isEmpty(rp) || ObjectUtils.isEmpty(rp.getAssignPosFlag()) || ObjectUtils.isEmpty(rp.getPositions())) {
            return new ArrayList<>();
        }
        return rp.getPositions().stream().map(rolePPositionPO -> {
            FlowPermissionPositionPO flowPermissionPositionPO = new FlowPermissionPositionPO();
            flowPermissionPositionPO.setPositionId(rolePPositionPO.getPositionId());
            flowPermissionPositionPO.setFlowpermissionId(fp.getId());
            return flowPermissionPositionPO;
        }).collect(Collectors.toList());
    }

}
