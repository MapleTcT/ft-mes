package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.rbac.common.exception.PermissionErrorEnum;
import com.supcon.supfusion.rbac.common.exception.PermissionException;
import com.supcon.supfusion.rbac.dao.*;
import com.supcon.supfusion.rbac.dao.bo.RolePermissionBO;
import com.supcon.supfusion.rbac.dao.field.MenuOperateField;
import com.supcon.supfusion.rbac.dao.field.RolePDepartmentField;
import com.supcon.supfusion.rbac.dao.field.RolePPositionField;
import com.supcon.supfusion.rbac.dao.field.RolePStaffField;
import com.supcon.supfusion.rbac.dao.field.RolePermissionField;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.dao.query.RolePDepartmentQuery;
import com.supcon.supfusion.rbac.dao.query.RolePPositionQuery;
import com.supcon.supfusion.rbac.dao.query.RolePStaffQuery;
import com.supcon.supfusion.rbac.dao.query.RolePermissionQuery;
import com.supcon.supfusion.rbac.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Slf4j
@Service
@Transactional
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermissionPO> implements IRolePermissionService {

    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private MenuoperateMapper menuoperateMapper;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private UserPPositionMapper userPPositionMapper;
    @Autowired
    private UserPStaffMapper userPStaffMapper;
    @Autowired
    private UserPDepartmentMapper userPDepartmentMapper;
    @Autowired
    private UserCustomPermissionRefMapper userCustomPermissionRefMapper;
    @Autowired
    private UserDataPermissionMapper userDataPermissionMapper;
    @Autowired
    private UserPermissionMapper userPermissionMapper;
    @Autowired
    private IUserPermissionService userPermissionService;
    @Autowired
    private IUserPPositionService userPPositionService;
    @Autowired
    private IUserPStaffService userPStaffService;
    @Autowired
    private IUserCustomPermissionRefService userCustomPermissionRefService;
    @Autowired
    private IUserDataPermissionService userDataPermissionService;
    @Autowired
    private IUserPDepartmentService userPDepartmentService;
    @Autowired
    private IMenuOperateService menuOperateService;

    @Autowired
    private RolePPositionMapper rolePPositionMapper;
    @Autowired
    private RolePDepartmentMapper rolePDepartmentMapper;
    @Autowired
    private RolePStaffMapper rolePStaffMapper;
    @Autowired
    private IRolePPositionService rolePPositionService;
    @Autowired
    private IRolePStaffService rolePStaffService;
    @Autowired
    private IRolePDepartmentService rolePDepartmentService;
    @Autowired
    private RoleCustomPermissionRefMapper roleCustomPermissionRefMapper;
    @Autowired
    private RoleDataPermissionMapper roleDataPermissionMapper;
    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private FlowPermissionStaffMapper flowPermissionStaffMapper;
    @Autowired
    private FlowPermissionPositionMapper flowPermissionPositionMapper;

    /**
     * @description: 获取角色权限列表
     * @param: roleId
     * @param: menuName
     * @param: operateName
     * @param: local
     * @return: java.util.List<com.supcon.supfusion.rbac.service.bo.RolePermissionBO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<RolePermissionBO> getRolePermissionList(Long roleId) {
        List<RolePermissionBO> allList;
        RolePermissionQuery rp = new RolePermissionQuery();
        rp.setRoleId(roleId);
        allList = rolePermissionMapper.getRolePermissionList(rp);
        Map<Long, RolePermissionBO> assignPosition = new HashMap<>();
        //需要查询指定人员的userPermission
        Map<Long,RolePermissionBO> assignStaff = new HashMap<>();
        //需要查询指定部门的userPermission
        Map<Long,RolePermissionBO> assignDepartment = new HashMap<>();
        allList.forEach(rolePermissionBO -> {
            if (rolePermissionBO.getAssignPosFlag()){
                assignPosition.put(rolePermissionBO.getId(),rolePermissionBO);
            }
            if (rolePermissionBO.getAssignStaffFlag()){
                assignStaff.put(rolePermissionBO.getId(),rolePermissionBO);
            }
            if (rolePermissionBO.getAssignDeptFlag()){
                assignDepartment.put(rolePermissionBO.getId(),rolePermissionBO);
            }
        });
        //为有指定岗位的权限填入岗位数据
        List<Long> assignPositionIds = new ArrayList<>(assignPosition.keySet());
        if (!ObjectUtils.isEmpty(assignPositionIds)){
            List<RolePPositionPO> result = new ArrayList<>();
            RolePPositionQuery rolePPositionQuery = new RolePPositionQuery();
            int batch = assignPositionIds.size() / 1000;
            if (batch == 0) {
                rolePPositionQuery.setRolePermissionIds(assignPositionIds);
                result = rolePPositionMapper.findByRolePermissionId(rolePPositionQuery);
            } else {
                for (int i = 0; i < batch; i++) {
                    rolePPositionQuery.setRolePermissionIds(assignPositionIds.subList(i * 1000, i * 1000 + 1000));
                    result.addAll(rolePPositionMapper.findByRolePermissionId(rolePPositionQuery));
                }
                if (assignPositionIds.size() % 1000 != 0) {
                    rolePPositionQuery.setRolePermissionIds(assignPositionIds.subList(batch * 1000, assignPositionIds.size()));
                    result.addAll(rolePPositionMapper.findByRolePermissionId(rolePPositionQuery));
                }
            }
            if(null!=result&&result.size()>0) {
            	result.forEach(pos -> assignPosition.get(pos.getRolepermissionId()).getPositions().add(pos));	
            }
           
        }
        //为有指定人员的权限填入人员数据
        List<Long> assignStaffIds = new ArrayList<>(assignStaff.keySet());
        if (!ObjectUtils.isEmpty(assignStaffIds)){
            List<RolePStaffPO> result = new ArrayList<>();
            RolePStaffQuery rolePStaffQuery = new RolePStaffQuery();
            int batch = assignStaffIds.size() / 1000;
            if (batch == 0) {
                rolePStaffQuery.setRolePermissionIds(assignStaffIds);
                result = rolePStaffMapper.findByRolePermissionId(rolePStaffQuery);
            } else {
                for (int i = 0; i < batch; i++) {
                    rolePStaffQuery.setRolePermissionIds(assignStaffIds.subList(i * 1000, i * 1000 + 1000));
                    result.addAll(rolePStaffMapper.findByRolePermissionId(rolePStaffQuery));
                }
                if (assignPositionIds.size() % 1000 != 0) {
                    rolePStaffQuery.setRolePermissionIds(assignStaffIds.subList(batch * 1000, assignStaffIds.size()));
                    result.addAll(rolePStaffMapper.findByRolePermissionId(rolePStaffQuery));
                }
            }
            if(null!=result&&result.size()>0) {
            	result.forEach(pos -> assignPosition.get(pos.getRolepermissionId()).getStaffs().add(pos));
            }
        }
        //为有指定部门的权限填入部门数据
        List<Long> assignDepartmentIds = new ArrayList<>(assignDepartment.keySet());
        if (!ObjectUtils.isEmpty(assignDepartmentIds)){
            List<RolePDepartmentPO> result = new ArrayList<>();
            RolePDepartmentQuery rolePDepartmentQuery = new RolePDepartmentQuery();
            int batch = assignDepartmentIds.size() / 1000;
            if (batch == 0) {
                rolePDepartmentQuery.setRolePermissionIds(assignDepartmentIds);
                result = rolePDepartmentMapper.findByRolePermissionId(rolePDepartmentQuery);
            } else {
                for (int i = 0; i < batch; i++) {
                    rolePDepartmentQuery.setRolePermissionIds(assignDepartmentIds.subList(i * 1000, i * 1000 + 1000));
                    result.addAll(rolePDepartmentMapper.findByRolePermissionId(rolePDepartmentQuery));
                }
                if (assignPositionIds.size() % 1000 != 0) {
                    rolePDepartmentQuery.setRolePermissionIds(assignDepartmentIds.subList(batch * 1000, assignDepartmentIds.size()));
                    result.addAll(rolePDepartmentMapper.findByRolePermissionId(rolePDepartmentQuery));
                }
            }
            if(null!=result&&result.size()>0) {
            	result.forEach(pos -> assignDepartment.get(pos.getRolepermissionId()).getDepartments().add(pos));
            }
        }
        return allList;
    }

    /**
     * @description: 保存角色权限
     * @param: rolePermissionPOList
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    @Transactional
    public void addOrUpdateRolePermission(List<RolePermissionPO> rolePermissionPOList) {
        if (CollectionUtils.isEmpty(rolePermissionPOList)) {
            return;
        }
        List<Long> operate_ids = rolePermissionPOList.stream().map(RolePermissionPO::getMenuOperateId).collect(Collectors.toList());
        // sql 超长处理
        QueryWrapper<MenuOperatePO> operatePOQueryWrapper = new QueryWrapper<>();
        int batch = operate_ids.size() / 1000;
        if (0 == batch) {
            operatePOQueryWrapper.in(MenuOperateField.id, operate_ids);
        } else {
            for (int i = 0; i < batch; i++) {
                operatePOQueryWrapper.or().in(MenuOperateField.id, operate_ids.subList(i * 1000, i * 1000 + 1000));
            }
            if (operate_ids.size() % 1000 != 0) {
                operatePOQueryWrapper.or().in(MenuOperateField.id, operate_ids.subList(batch * 1000, operate_ids.size()));
            }
        }
        List<MenuOperatePO> operatePOS = menuOperateService.list(operatePOQueryWrapper);
        //构造map
        Map<Long,MenuOperatePO> operateMap = new HashMap<>();
        operatePOS.forEach(menuOperatePO -> {
            operateMap.put(menuOperatePO.getId(),menuOperatePO);
        });

        //新增工作流权限
        flowPermissionService.insertFlowPermissionByRolePermission(rolePermissionPOList,operateMap);

        List<RolePermissionPO> unflowPermission = rolePermissionPOList.stream().filter(rolePermissionPO -> {
            MenuOperatePO operatePO = operateMap.get(rolePermissionPO.getMenuOperateId());
            return ObjectUtils.isEmpty(operatePO.getFlowKey());
        }).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(unflowPermission)){
            return;
        }
        unflowPermission.forEach(userPermissionPO -> {
            userPermissionPO.setCid(UserContext.getUserContext().getCompanyId());
            if (ObjectUtils.isEmpty(userPermissionPO.getAssignDeptFlag()) && ObjectUtils.isEmpty(userPermissionPO.getPositionFlag()) && ObjectUtils.isEmpty(userPermissionPO.getDepartmentFlag())
                    && ObjectUtils.isEmpty(userPermissionPO.getAssignStaffFlag()) && ObjectUtils.isEmpty(userPermissionPO.getAssignPosFlag()) && ObjectUtils.isEmpty(userPermissionPO.getAssignPosFlag())){
                userPermissionPO.setNoRestrictFlag(true);
            }
        });

        unflowPermission.forEach(rolePermissionPO -> {
            if (ObjectUtils.isEmpty(rolePermissionPO.getAssignDeptFlag()) && ObjectUtils.isEmpty(rolePermissionPO.getPositionFlag()) && ObjectUtils.isEmpty(rolePermissionPO.getDepartmentFlag())
                    && ObjectUtils.isEmpty(rolePermissionPO.getAssignStaffFlag()) && ObjectUtils.isEmpty(rolePermissionPO.getAssignPosFlag()) && ObjectUtils.isEmpty(rolePermissionPO.getAssignPosFlag())){
                rolePermissionPO.setNoRestrictFlag(true);
            }
        });
        // 保存角色修改的相关权限数据
        saveOrUpdateBatch(unflowPermission);

        List<Long> rolePermission_ids = unflowPermission.stream().map(RolePermissionPO::getId).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(rolePermission_ids)) {
            // sql 超长处理
            QueryWrapper<RolePDepartmentPO> rolePDepartmentPOQueryWrapper = new QueryWrapper<>();
            QueryWrapper<RolePStaffPO> rolePStaffPOQueryWrapper = new QueryWrapper<>();
            QueryWrapper<RolePPositionPO> rolePPositionPOQueryWrapper = new QueryWrapper<>();
            int rolePermBatch = rolePermission_ids.size() / 1000;
            if (0 == rolePermBatch) {
                rolePDepartmentPOQueryWrapper.in(RolePDepartmentField.rolepermissionId, rolePermission_ids);
                rolePStaffPOQueryWrapper.in(RolePStaffField.rolepermissionId, rolePermission_ids);
                rolePPositionPOQueryWrapper.in(RolePPositionField.rolepermissionId, rolePermission_ids);
            } else {
                for (int i = 0; i < batch; i++) {
                    rolePDepartmentPOQueryWrapper.or().in(RolePDepartmentField.rolepermissionId, rolePermission_ids.subList(i * 1000, i * 1000 + 1000));
                    rolePStaffPOQueryWrapper.or().in(RolePStaffField.rolepermissionId, rolePermission_ids.subList(i * 1000, i * 1000 + 1000));
                    rolePPositionPOQueryWrapper.or().in(RolePPositionField.rolepermissionId, rolePermission_ids.subList(i * 1000, i * 1000 + 1000));
                }
                if (rolePermission_ids.size() % 1000 != 0) {
                    rolePDepartmentPOQueryWrapper.or().in(RolePDepartmentField.rolepermissionId, rolePermission_ids.subList(batch * 1000, rolePermission_ids.size()));
                    rolePStaffPOQueryWrapper.or().in(RolePStaffField.rolepermissionId, rolePermission_ids.subList(batch * 1000, rolePermission_ids.size()));
                    rolePPositionPOQueryWrapper.or().in(RolePPositionField.rolepermissionId, rolePermission_ids.subList(batch * 1000, rolePermission_ids.size()));
                }
            }
            rolePDepartmentService.remove(rolePDepartmentPOQueryWrapper);
            rolePStaffService.remove(rolePStaffPOQueryWrapper);
            rolePPositionService.remove(rolePPositionPOQueryWrapper);
        }
        Long roleId = unflowPermission.get(0).getRoleId();
        List<Long> opIds = new ArrayList<>();
        for (RolePermissionPO rolePermissionPO : unflowPermission) {
            opIds.add(rolePermissionPO.getMenuOperateId());
            if (rolePermissionPO.getAssignPosFlag() != null && rolePermissionPO.getAssignPosFlag()) {
                if (!ObjectUtils.isEmpty(rolePermissionPO.getPositions())) {
                    // 指定岗位数据
                    rolePermissionPO.getPositions().forEach(rolePPositionPO -> {
                        rolePPositionPO.setRolepermissionId(rolePermissionPO.getId());
                        rolePPositionPO.setIncludeLower(false);
                    });
                    modifyPosition(rolePermissionPO.getPositions());
                }else{
                    throw new PermissionException(PermissionErrorEnum.ASSIGN_ERROR);
                }
            }
            if (rolePermissionPO.getAssignDeptFlag() != null && rolePermissionPO.getAssignDeptFlag()) {
                if (!ObjectUtils.isEmpty(rolePermissionPO.getDepartments())) {
                    // 指定部门数据
                    rolePermissionPO.getDepartments().forEach(rolePDepartmentPO -> {
                        rolePDepartmentPO.setRolepermissionId(rolePermissionPO.getId());
                        rolePDepartmentPO.setIncludeLower(false);
                    });
                    modifyDepartment(rolePermissionPO.getDepartments());
                }else{
                    throw new PermissionException(PermissionErrorEnum.ASSIGN_ERROR);
                }
            }
            if (rolePermissionPO.getAssignStaffFlag() != null && rolePermissionPO.getAssignStaffFlag()) {
                if (!ObjectUtils.isEmpty(rolePermissionPO.getStaffs())) {
                    // 指定人员数据
                    rolePermissionPO.getStaffs().forEach(rolePStaffPO -> {
                        rolePStaffPO.setRolepermissionId(rolePermissionPO.getId());
                    });
                    modifyStaff(rolePermissionPO.getStaffs());
                }else{
                    throw new PermissionException(PermissionErrorEnum.ASSIGN_ERROR);
                }
            }
        }
        // 刷新用户相关权限数据
        freshSubOperate(roleId, opIds, null, null);

    }

    /**
     * @description: 修改角色权限指定岗位
     * @param: staffs
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/9
     */
    private void modifyPosition(List<RolePPositionPO> positions) {
        if (CollectionUtils.isEmpty(positions)) {
            return;
        }
        // 解析岗位相关数据并插入用户岗位关联表
        rolePPositionService.saveBatch(positions);
    }

    /**
     * @description: 修改角色权限指定部门
     * @param: staffs
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/9
     */
    private void modifyDepartment(List<RolePDepartmentPO> departments) {
        if (CollectionUtils.isEmpty(departments)) {
            return;
        }
        // 解析岗位相关数据并插入用户部门关联表
        rolePDepartmentService.saveBatch(departments);
    }

    /**
     * @description: 修改角色权限指定人员
     * @param: staffs
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/9
     */
    private void modifyStaff(List<RolePStaffPO> staffs) {
        if (CollectionUtils.isEmpty(staffs)) {
            return;
        }
        // 解析人员相关数据并插入用户人员关联表
        rolePStaffService.saveBatch(staffs);
    }

    /**
     * @description: 角色权限数据刷新到用户权限表中
     * @param: rid
     * @param: opIds
     * @param: userId
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    @Transactional
    public void freshSubOperate(Long rid, List<Long> opIds, Long userId, Long arg_cid) {
        System.out.println("开始刷权限");
        RolePO role = roleService.getById(rid);
        if (!ObjectUtils.isEmpty(role)){
            arg_cid = role.getCid();
        }
        Long cid = arg_cid;
        //清除老数据
//        userPPositionMapper.deleteUserPPosition(cid, opIds, rid, userId);
//        userPStaffMapper.deleteUserPStaff(cid, opIds, rid, userId);
//        userCustomPermissionRefMapper.deleteUserCustomPermission(cid, opIds, rid, userId);
//        userDataPermissionMapper.deleteUserDataPermission(cid, opIds, rid, userId);
//        userPDepartmentMapper.deleteUserPDepartment(cid,opIds,rid,userId);
//        userPermissionMapper.deleteUserPermission(cid, opIds, rid, userId);
        int batch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            batch = opIds.size() / 1000;
        }
        if (0 == batch) {
            userPPositionMapper.deleteUserPPosition(cid, opIds, rid, userId);
            userPStaffMapper.deleteUserPStaff(cid, opIds, rid, userId);
            userCustomPermissionRefMapper.deleteUserCustomPermission(cid, opIds, rid, userId);
            userDataPermissionMapper.deleteUserDataPermission(cid, opIds, rid, userId);
            userPDepartmentMapper.deleteUserPDepartment(cid, opIds, rid, userId);
            userPermissionMapper.deleteUserPermission(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < batch; i++) {
                userPPositionMapper.deleteUserPPosition(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId);
                userPStaffMapper.deleteUserPStaff(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId);
                userCustomPermissionRefMapper.deleteUserCustomPermission(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId);
                userDataPermissionMapper.deleteUserDataPermission(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId);
                userPDepartmentMapper.deleteUserPDepartment(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId);
                userPermissionMapper.deleteUserPermission(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId);
            }
            if (opIds.size() % 1000 != 0) {
                userPPositionMapper.deleteUserPPosition(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId);
                userPStaffMapper.deleteUserPStaff(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId);
                userCustomPermissionRefMapper.deleteUserCustomPermission(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId);
                userDataPermissionMapper.deleteUserDataPermission(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId);
                userPDepartmentMapper.deleteUserPDepartment(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId);
                userPermissionMapper.deleteUserPermission(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId);
            }
        }
        /**
         * oracle sql超长处理
         */
        //查询新的权限数据,当有一个人有多个角色时，需要对权限的计算（岗位限制、组限制、指定岗位、指定人员、处理人权限）
        saveUserPermission(rid, opIds, userId, cid, batch);
        // 查询用户权限 指定岗位数据 若一个人有多个角色，并且每个角色下都有指定岗位，若其中有一个有包含下级的，则该权限就包含下级岗位
        saveUserPPosition(rid, opIds, userId, cid, batch);
        // 查询用户权限 指定人员数据
        saveUserPStaff(rid, opIds, userId, cid, batch);
        // 查询用户权限 指定部门数据
        saveUserPDepartment(rid, opIds, userId, cid, batch);
        // 查询用户权限 指定其他限制(自定义权限)
        saveUserCustomPermissionRef(rid, opIds, userId, cid, batch);
        // 查询用户权限 指定其他限制(自定义权限)
        saveUserDataPermission(rid, opIds, userId, cid, batch);


    }


    private void saveUserPermission(Long rid, List<Long> opIds, Long userId, Long cid, int batch) {
        //查询新的权限数据,当有一个人有多个角色时，需要对权限的计算（岗位限制、组限制、指定岗位、指定人员、处理人权限）
//        List<Map<String, Object>> newPermissionList = rolePermissionMapper.getNewPermissionList(cid, opIds, rid, userId);
        // sql 超长问题处理
        List<Map<String, Object>> newPermissionList = new ArrayList<>();
        int opIdsBatch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            opIdsBatch = opIds.size() / 1000;
        }
        if (0 == opIdsBatch) {
            newPermissionList = rolePermissionMapper.getNewPermissionList(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < opIdsBatch; i++) {
                newPermissionList.addAll(rolePermissionMapper.getNewPermissionList(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId));
            }
            if (opIds.size() % 1000 != 0) {
                newPermissionList.addAll(rolePermissionMapper.getNewPermissionList(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId));
            }
        }
        List<UserPermissionPO> batchSave = new ArrayList<>();
        newPermissionList.forEach(stringObjectMap -> {
            UserPermissionPO userPermissionPO = new UserPermissionPO();
            userPermissionPO.setCid(cid);
            userPermissionPO.setUserId((long) stringObjectMap.get("USER_ID"));
            if (!ObjectUtils.isEmpty(stringObjectMap.get("DEALER_PERMISSION_FLAG"))) {
                userPermissionPO.setDealerPermissionFlag(1 == ((int) stringObjectMap.get("DEALER_PERMISSION_FLAG")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("NO_RESTRICT_FLAG"))) {
                userPermissionPO.setNoRestrictFlag(1 == ((int) stringObjectMap.get("NO_RESTRICT_FLAG")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("ASSIGN_STAFF_FLAG"))) {
                userPermissionPO.setAssignStaffFlag(1 == ((int) stringObjectMap.get("ASSIGN_STAFF_FLAG")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("ASSIGN_POS_FLAG"))) {
                userPermissionPO.setAssignPosFlag(1 == ((int) stringObjectMap.get("ASSIGN_POS_FLAG")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("ASSIGN_DEPT_FLAG"))) {
                userPermissionPO.setAssignDeptFlag(1 == ((int) stringObjectMap.get("ASSIGN_DEPT_FLAG")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("POSITION_FLAG"))) {
                userPermissionPO.setPositionFlag(1 == ((int) stringObjectMap.get("POSITION_FLAG")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("URL_PATTERN"))) {
                userPermissionPO.setUrlPattern(String.valueOf(stringObjectMap.get("URL_PATTERN")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("MENUOPERATE_ID"))) {
                userPermissionPO.setMenuOperateId((long) stringObjectMap.get("MENUOPERATE_ID"));
            }
            userPermissionPO.setPurviewType(0);
            if (!ObjectUtils.isEmpty(stringObjectMap.get("GROUP_FLAG"))) {
                userPermissionPO.setGroupFlag((int) stringObjectMap.get("GROUP_FLAG"));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("CODE"))) {
                userPermissionPO.setMenuOperateCode(String.valueOf(stringObjectMap.get("CODE")));
            }
            if (!ObjectUtils.isEmpty(stringObjectMap.get("DEPARTMENT_FLAG"))) {
                userPermissionPO.setDepartmentFlag(1 == ((int) stringObjectMap.get("DEPARTMENT_FLAG")));
            }
            batchSave.add(userPermissionPO);
        });
        userPermissionService.saveBatch(batchSave);
    }


    private void saveUserPPosition(Long rid, List<Long> opIds, Long userId, Long cid, int batch) {
        // 查询用户权限 指定岗位数据 若一个人有多个角色，并且每个角色下都有指定岗位，若其中有一个有包含下级的，则该权限就包含下级岗位
//        List<Map<String, Object>> newPositionPermissionList = userPermissionMapper.getNewPositionPermissionList(cid, opIds, rid, userId);
        // sql 超长处理
        List<Map<String, Object>> newPositionPermissionList = new ArrayList<>();
        int opIdsBatch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            opIdsBatch = opIds.size() / 1000;
        }
        if (0 == opIdsBatch) {
            newPositionPermissionList = userPermissionMapper.getNewPositionPermissionList(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < opIdsBatch; i++) {
                newPositionPermissionList.addAll(userPermissionMapper.getNewPositionPermissionList(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId));
            }
            if (opIds.size() % 1000 != 0) {
                newPositionPermissionList.addAll(userPermissionMapper.getNewPositionPermissionList(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId));
            }
        }

        List<UserPPositionPO> batchSaveUpp = new ArrayList<>();
        newPositionPermissionList.forEach(stringObjectMap -> {
            UserPPositionPO upp = new UserPPositionPO();
            UserPermissionPO up = new UserPermissionPO();
            up.setId((Long) stringObjectMap.get("uid"));
            upp.setUserpermissionId(up.getId());
            upp.setPositionId((Long) stringObjectMap.get("pid"));
            upp.setIncludeLower(1 == (int) stringObjectMap.get("includeLower"));
            batchSaveUpp.add(upp);
        });
        userPPositionService.saveBatch(batchSaveUpp);
    }




    private void saveUserDataPermission(Long rid, List<Long> opIds, Long userId, Long cid, int batch) {
        // 查询用户权限 指定其他限制(自定义权限)
        List<Map<String, Object>> newDataPermissionList = new ArrayList<>();
        int opIdsBatch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            opIdsBatch = opIds.size() / 1000;
        }
        if (0 == opIdsBatch) {
            newDataPermissionList = userPermissionMapper.getNewDataPermissionList(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < opIdsBatch; i++) {
                newDataPermissionList.addAll(userPermissionMapper.getNewDataPermissionList(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId));
            }
            if (opIds.size() % 1000 != 0) {
                newDataPermissionList.addAll(userPermissionMapper.getNewDataPermissionList(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId));
            }
        }
        // 查询用户权限 指定特殊权限
//        List<Map<String, Object>> newDataPermissionList = userPermissionMapper.getNewDataPermissionList(cid, opIds, rid, userId);
        List<UserDataPermissionPO> batchSaveDps = new ArrayList<>();
        newDataPermissionList.forEach(stringObjectMap -> {
            UserDataPermissionPO dp = new UserDataPermissionPO();
            dp.setUserpermissionId((Long) stringObjectMap.get("uid"));
            dp.setDataPermissionCode((String) stringObjectMap.get("dpc"));
            dp.setContent((String) stringObjectMap.get("content"));
            batchSaveDps.add(dp);
        });
        userDataPermissionService.saveBatch(batchSaveDps);
    }

    private void saveUserCustomPermissionRef(Long rid, List<Long> opIds, Long userId, Long cid, int batch) {
        // 查询用户权限 指定其他限制(自定义权限)
//        List<Map<String, Object>> newCustomPermissionList = userPermissionMapper.getNewCustomPermissionList(cid, opIds, rid, userId);
        List<Map<String, Object>> newCustomPermissionList = new ArrayList<>();
        int opIdsBatch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            opIdsBatch = opIds.size() / 1000;
        }
        if (0 == opIdsBatch) {
            newCustomPermissionList = userPermissionMapper.getNewCustomPermissionList(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < opIdsBatch; i++) {
                newCustomPermissionList.addAll(userPermissionMapper.getNewCustomPermissionList(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId));
            }
            if (opIds.size() % 1000 != 0) {
                newCustomPermissionList.addAll(userPermissionMapper.getNewCustomPermissionList(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId));
            }
        }
        List<UserCustomPermissionRefPO> batchSaveCps = new ArrayList<>();
        newCustomPermissionList.forEach(stringObjectMap -> {
            UserCustomPermissionRefPO cp = new UserCustomPermissionRefPO();
            cp.setUserpermissionId((Long) stringObjectMap.get("uid"));
            cp.setCustomPermissionCode((String) stringObjectMap.get("cpc"));
            batchSaveCps.add(cp);
        });
        userCustomPermissionRefService.saveBatch(batchSaveCps);
    }

    private void saveUserPDepartment(Long rid, List<Long> opIds, Long userId, Long cid, int batch) {
        // 查询用户权限 指定部门数据
//        List<Map<String, Object>> newDeptPermissionList = userPermissionMapper.getNewDeptPermissionList(cid, opIds, rid, userId);
        List<Map<String, Object>> newDeptPermissionList = new ArrayList<>();
        int opIdsBatch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            opIdsBatch = opIds.size() / 1000;
        }
        if (0 == opIdsBatch) {
            newDeptPermissionList = userPermissionMapper.getNewDeptPermissionList(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < opIdsBatch; i++) {
                newDeptPermissionList.addAll(userPermissionMapper.getNewDeptPermissionList(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId));
            }
            if (opIds.size() % 1000 != 0) {
                newDeptPermissionList.addAll(userPermissionMapper.getNewDeptPermissionList(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId));
            }
        }
        List<UserPDepartmentPO> batchSaveDepts = new ArrayList<>();
        newDeptPermissionList.forEach(stringObjectMap -> {
            UserPDepartmentPO upd = new UserPDepartmentPO();
            upd.setDepartmentId((Long) stringObjectMap.get("did"));
            upd.setUserpermissionId((Long) stringObjectMap.get("uid"));
            batchSaveDepts.add(upd);
        });
        userPDepartmentService.saveBatch(batchSaveDepts);
    }

    private void saveUserPStaff(Long rid, List<Long> opIds, Long userId, Long cid, int batch) {
        // 查询用户权限 指定人员数据
//        List<Map<String, Object>> newStaffPermissionList = userPermissionMapper.getNewStaffPermissionList(cid, opIds, rid, userId);
        //sql 超长处理
        List<Map<String, Object>> newStaffPermissionList = new ArrayList<>();
        int opIdsBatch = 0;
        if (!CollectionUtils.isEmpty(opIds)) {
            opIdsBatch = opIds.size() / 1000;
        }
        if (0 == opIdsBatch) {
            newStaffPermissionList = userPermissionMapper.getNewStaffPermissionList(cid, opIds, rid, userId);
        } else {
            for (int i = 0; i < opIdsBatch; i++) {
                newStaffPermissionList.addAll(userPermissionMapper.getNewStaffPermissionList(cid, opIds.subList(i * 1000, i * 1000 + 1000), rid, userId));
            }
            if (opIds.size() % 1000 != 0) {
                newStaffPermissionList.addAll(userPermissionMapper.getNewStaffPermissionList(cid, opIds.subList(batch * 1000, opIds.size()), rid, userId));
            }
        }

        List<UserPStaffPO> batchSaveUps = new ArrayList<>();
        newStaffPermissionList.forEach(stringObjectMap -> {
            UserPStaffPO ups = new UserPStaffPO();
            ups.setUserpermissionId((Long) stringObjectMap.get("uid"));
            ups.setStaffId((Long) stringObjectMap.get("sid"));
            batchSaveUps.add(ups);
        });
        userPStaffService.saveBatch(batchSaveUps);
    }





    /**
     * @description: 批量删除角色权限
     * @param: rpList
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void batchDeleteRolePermissions(List<RolePermissionPO> rpList) {
        List<Long> ids = rpList.stream().map(RolePermissionPO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        // 删除角色权限相关数据
        // sql 超长问题处理
        QueryWrapper<RolePermissionPO> rolePermissionPOQueryWrapper = new QueryWrapper<>();
        QueryWrapper<RolePPositionPO> rolePPositionPOQueryWrapper = new QueryWrapper<>();
        QueryWrapper<RolePStaffPO> rolePStaffPOQueryWrapper = new QueryWrapper<>();
        QueryWrapper<RolePDepartmentPO> rolePDepartmentPOQueryWrapper = new QueryWrapper<>();
        int batch = ids.size() / 1000;
        if (0 == batch) {
            rolePermissionPOQueryWrapper.in(RolePermissionField.id, ids);
            rolePPositionPOQueryWrapper.in("ROLEPERMISSION_ID", ids);
            rolePStaffPOQueryWrapper.in("ROLEPERMISSION_ID", ids);
            rolePDepartmentPOQueryWrapper.in("ROLEPERMISSION_ID", ids);
        } else {
            for (int i = 0; i < batch; i++) {
                rolePermissionPOQueryWrapper.or().in(RolePermissionField.id, ids.subList(i * 1000, i * 1000 + 1000));
                rolePPositionPOQueryWrapper.or().in("ROLEPERMISSION_ID", ids.subList(i * 1000, i * 1000 + 1000));
                rolePStaffPOQueryWrapper.or().in("ROLEPERMISSION_ID", ids.subList(i * 1000, i * 1000 + 1000));
                rolePDepartmentPOQueryWrapper.or().in("ROLEPERMISSION_ID", ids.subList(i * 1000, i * 1000 + 1000));
            }
            if (ids.size() % 1000 != 0) {
                rolePermissionPOQueryWrapper.or().in(RolePermissionField.id, ids.subList(batch * 1000, ids.size()));
                rolePPositionPOQueryWrapper.or().in("ROLEPERMISSION_ID", ids.subList(batch * 1000, ids.size()));
                rolePStaffPOQueryWrapper.or().in("ROLEPERMISSION_ID", ids.subList(batch * 1000, ids.size()));
                rolePDepartmentPOQueryWrapper.or().in("ROLEPERMISSION_ID", ids.subList(batch * 1000, ids.size()));
            }
        }
        // 删除角色权限相关数据
        remove(rolePermissionPOQueryWrapper);
        // 删除角色岗位相关数据
        rolePPositionMapper.delete(rolePPositionPOQueryWrapper);
        // 删除角色人员相关数据
        rolePStaffMapper.delete(rolePStaffPOQueryWrapper);
        // 删除角色部门相关数据
        rolePDepartmentMapper.delete(rolePDepartmentPOQueryWrapper);

        //删除工作流权限
        int rpBatch = rpList.size() / 1000;
        if (0 == rpBatch) {
            flowPermissionService.deleteFlowPermissionByRolePermission(rpList);
        } else {
            for (int i = 0; i < rpBatch; i++) {
                flowPermissionService.deleteFlowPermissionByRolePermission(rpList.subList(i * 1000, i * 1000 + 1000));
            }
            if (rpList.size() % 1000 != 0) {
                flowPermissionService.deleteFlowPermissionByRolePermission(rpList.subList(rpBatch * 1000, rpList.size()));
            }
        }
    }

    /**
     * @description: 给角色授权
     * @param: roleId
     * @param: menuOperateCodes
     * @param: cid
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void grantPermission(Long roleId, List<String> menuOperateCodes, Long cid) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("CODE",menuOperateCodes);
        List<MenuOperatePO> operatePOS = menuoperateMapper.selectList(queryWrapper);
        List<RolePermissionPO> rolePermissionPOS = operatePOS.stream().map(menuOperatePO -> {
            RolePermissionPO rolePermissionPO = new RolePermissionPO();
            rolePermissionPO.setRoleId(roleId);
            rolePermissionPO.setNoRestrictFlag(true);
            rolePermissionPO.setMenuOperateId(menuOperatePO.getId());
            rolePermissionPO.setCid(cid);
            return rolePermissionPO;
        }).collect(Collectors.toList());
        saveBatch(rolePermissionPOS);
    }

}
