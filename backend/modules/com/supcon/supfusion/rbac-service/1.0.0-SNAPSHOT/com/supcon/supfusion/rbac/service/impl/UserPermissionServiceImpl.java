package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.rbac.common.exception.PermissionErrorEnum;
import com.supcon.supfusion.rbac.common.exception.PermissionException;
import com.supcon.supfusion.rbac.dao.*;
import com.supcon.supfusion.rbac.dao.bo.RolePermissionBO;
import com.supcon.supfusion.rbac.dao.enums.FlowPermissionType;
import com.supcon.supfusion.rbac.dao.field.*;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.dao.query.UserPDepartmentQuery;
import com.supcon.supfusion.rbac.dao.query.UserPPositionQuery;
import com.supcon.supfusion.rbac.dao.query.UserPStaffQuery;
import com.supcon.supfusion.rbac.dao.query.UserPermissionQuery;
import com.supcon.supfusion.rbac.service.*;
import com.supcon.supfusion.rbac.service.bo.PrivilegeBO;
import com.supcon.supfusion.rbac.dao.bo.UserPermissionBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
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
public class UserPermissionServiceImpl extends ServiceImpl<UserPermissionMapper, UserPermissionPO> implements IUserPermissionService {

    @Autowired
    private UserPermissionMapper userPermissionMapper;
    @Autowired
    private UserPPositionMapper userPPositionMapper;
    @Autowired
    private UserPStaffMapper userPStaffMapper;
    @Autowired
    private UserPDepartmentMapper userPDepartmentMapper;
    @Autowired
    private IUserPPositionService userPPositionService;
    @Autowired
    private IUserPStaffService userPStaffService;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private IUserPDepartmentService userPDepartmentService;
    @Autowired
    private UserDataPermissionMapper userDataPermissionMapper;
    @Autowired
    private UserCustomPermissionRefMapper userCustomPermissionRefMapper;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private FlowPermissionStaffMapper flowPermissionStaffMapper;
    @Autowired
    private FlowPermissionPositionMapper flowPermissionPositionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    /**
     * @description: 根据用户ID查询用户权限
     * @param: userId
     * @param: menuName
     * @param: operateName
     * @param: local
     * @return: java.util.List<com.supcon.supfusion.rbac.service.bo.UserPermissionBO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    //TODO 待优化，IO还可减少
    @Override
    public List<UserPermissionBO> getUserPermissionListFull(Long userId,Integer purviewType) {
        UserContext userContext = UserContext.getUserContext();
        UserPermissionQuery userPermissionQuery = new UserPermissionQuery();
        userPermissionQuery.setUserId(userId);
        userPermissionQuery.setCid(userContext.getCompanyId());
        userPermissionQuery.setPurviewType(purviewType);
        //查询用户权限
        List<UserPermissionBO> allList = userPermissionMapper.getUserPermissionList(userPermissionQuery);
        //需要查询指定岗位的userPermission
        Map<Long,UserPermissionBO> assignPosition = new HashMap<>();
        //需要查询指定人员的userPermission
        Map<Long,UserPermissionBO> assignStaff = new HashMap<>();
        //需要查询指定部门的userPermission
        Map<Long,UserPermissionBO> assignDepartment = new HashMap<>();
        allList.forEach(userPermissionBO -> {
            if (userPermissionBO.getAssignPosFlag()){
                assignPosition.put(userPermissionBO.getId(),userPermissionBO);
            }
            if (userPermissionBO.getAssignStaffFlag()){
                assignStaff.put(userPermissionBO.getId(),userPermissionBO);
            }
            if (userPermissionBO.getAssignDeptFlag()){
                assignDepartment.put(userPermissionBO.getId(),userPermissionBO);
            }
        });
        //为有指定岗位的权限填入岗位数据
        List<Long> assignPositionIds = new ArrayList<>(assignPosition.keySet());
        if (!ObjectUtils.isEmpty(assignPositionIds)){
            UserPPositionQuery userPPositionQuery = new UserPPositionQuery();
            userPPositionQuery.setUserPermissionIds(assignPositionIds);
            List<UserPPositionPO> userPPositionPOS = userPPositionMapper.findByUserPermissionId(userPPositionQuery);
            userPPositionPOS.forEach(pos -> {
                assignPosition.get(pos.getUserpermissionId()).getPositions().add(pos);
            });
        }
        //为有指定人员的权限填入人员数据
        List<Long> assignStaffIds = new ArrayList<>(assignStaff.keySet());
        if (!ObjectUtils.isEmpty(assignStaffIds)){
            UserPStaffQuery userPStaffQuery = new UserPStaffQuery();
            userPStaffQuery.setUserPermissionIds(assignStaffIds);
            List<UserPStaffPO> userPStaffPOS = userPStaffMapper.findByUserPermissionId(userPStaffQuery);
            userPStaffPOS.forEach(pos -> {
                assignStaff.get(pos.getUserpermissionId()).getStaffs().add(pos);
            });
        }
        //为有指定部门的权限填入部门数据
        List<Long> assignDepartmentIds = new ArrayList<>(assignDepartment.keySet());
        if (!ObjectUtils.isEmpty(assignDepartmentIds)){
            UserPDepartmentQuery userPDepartmentQuery = new UserPDepartmentQuery();
            userPDepartmentQuery.setUserPermissionIds(assignDepartmentIds);
            List<UserPDepartmentPO> userPDepartmentPOS = userPDepartmentMapper.findByUserPermissionId(userPDepartmentQuery);
            userPDepartmentPOS.forEach(pos -> {
                assignDepartment.get(pos.getUserpermissionId()).getDepartments().add(pos);
            });
        }
        //如果是角色里来的，需要查询角色来源
        if (purviewType == 0){
            List<RolePermissionBO> rolePermissionBOS = rolePermissionMapper.findRolePermissionByUserId(userId);
            Map<Long, List<RolePermissionBO>> groupBOS = rolePermissionBOS.stream().collect(Collectors.groupingBy(RolePermissionBO::getMenuOperateId));
            allList.forEach(userPermissionBO -> {
                List<RolePermissionBO> bos = groupBOS.get(userPermissionBO.getMenuOperateId());
                if (ObjectUtils.isEmpty(bos)){
                    return;
                }
                List<RolePO> roles = bos.stream().map(RolePermissionBO::getRole).collect(Collectors.toList());
                userPermissionBO.setRoles(roles);
            });
        }
        return allList;
    }

    /**
     * @description: 批量删除用户权限
     * @param: upList
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    @Transactional
    public void batchDeleteUserPermissions(List<UserPermissionPO> upList) {
        //modify by yy 2020/6/28 增加非空判断
        if (!ObjectUtils.isEmpty(upList)) {
            List<Long> ids = upList.stream().map(UserPermissionPO::getId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ids)) {
                return;
            }
            QueryWrapper<UserPPositionPO> userPPositionPOQueryWrapper = new QueryWrapper<>();
            QueryWrapper<UserPStaffPO> userPStaffPOQueryWrapper = new QueryWrapper<>();
            QueryWrapper<UserPDepartmentPO> userPDepartmentPOQueryWrapper = new QueryWrapper<>();

            //sql 超过长度处理
            int batch = ids.size() / 1000;
            if (0 == batch) {
                removeByIds(ids);
                userPPositionPOQueryWrapper.in("USERPERMISSION_ID", ids);
                userPStaffPOQueryWrapper.in("USERPERMISSION_ID", ids);
                userPDepartmentPOQueryWrapper.in("USERPERMISSION_ID", ids);
            } else {
                for (int i = 0; i < batch; i++) {
                    removeByIds(ids.subList(i * 1000, i * 1000 + 1000));
                    userPPositionPOQueryWrapper.or().in("USERPERMISSION_ID", ids.subList(i * 1000, i * 1000 + 1000));
                    userPStaffPOQueryWrapper.or().in("USERPERMISSION_ID", ids.subList(i * 1000, i * 1000 + 1000));
                    userPDepartmentPOQueryWrapper.or().in("USERPERMISSION_ID", ids.subList(i * 1000, i * 1000 + 1000));
                }
                if (ids.size() % 1000 != 0) {
                    removeByIds(ids.subList(batch * 1000, ids.size()));
                    userPPositionPOQueryWrapper.or().in("USERPERMISSION_ID", ids.subList(batch * 1000, ids.size()));
                    userPStaffPOQueryWrapper.or().in("USERPERMISSION_ID", ids.subList(batch * 1000, ids.size()));
                    userPDepartmentPOQueryWrapper.or().in("USERPERMISSION_ID", ids.subList(batch * 1000, ids.size()));
                }
            }
            // 删除用户权限相关数据
            // 删除用户岗位相关数据
            userPPositionMapper.delete(userPPositionPOQueryWrapper);
            // 删除用户人员相关数据
            userPStaffMapper.delete(userPStaffPOQueryWrapper);
            // 删除用户部门相关数据
            userPDepartmentMapper.delete(userPDepartmentPOQueryWrapper);
            //删除工作流权限相关数据
            flowPermissionService.deleteFlowPermissionByUserPermission(upList);
        }
    }

    /**
     * @description: 保存用户权限
     * @param: userPermissionPOList
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    @Transactional
    public void addOrUpdateUserPermission(List<UserPermissionPO> userPermissionPOList) {
        if (CollectionUtils.isEmpty(userPermissionPOList)) {
            return;
        }
        List<Long> operate_ids = userPermissionPOList.stream().map(UserPermissionPO::getMenuOperateId).collect(Collectors.toList());
//        List<MenuOperatePO> operatePOS = menuOperateService.list(new QueryWrapper<MenuOperatePO>().in(MenuOperateField.id, operate_ids));
        //操作超过1000个进行处理
        QueryWrapper<MenuOperatePO> childMenuOpWrapper =new QueryWrapper<MenuOperatePO>();        
        int batch = operate_ids.size() / 1000;
        if (batch == 0) {
        	childMenuOpWrapper.in(MenuOperateField.id, operate_ids);
        } else {
            for (int i = 0; i < batch; i++) {
                childMenuOpWrapper.or().in(MenuOperateField.id, operate_ids.subList(i * 1000, i * 1000 + 1000));
            }
            if (operate_ids.size() % 1000 != 0) {
                childMenuOpWrapper.or().in(MenuOperateField.id, operate_ids.subList(batch * 1000, operate_ids.size()));
            }
        }
        List<MenuOperatePO> operatePOS=menuOperateService.list(childMenuOpWrapper);
        //构造权限数据
        Map<Long,MenuOperatePO> operateMap = new HashMap<>();
        operatePOS.forEach(menuOperatePO -> operateMap.put(menuOperatePO.getId(),menuOperatePO));
        List<UserPermissionPO> unFlowPermission = userPermissionPOList.stream().filter(userPermissionPO -> {
            MenuOperatePO operatePO = operateMap.get(userPermissionPO.getMenuOperateId());
            if (ObjectUtils.isEmpty(operatePO.getFlowKey())){
                userPermissionPO.setCid(UserContext.getUserContext().getCompanyId());
                if (ObjectUtils.isEmpty(userPermissionPO.getAssignDeptFlag()) && ObjectUtils.isEmpty(userPermissionPO.getPositionFlag()) && ObjectUtils.isEmpty(userPermissionPO.getDepartmentFlag())
                        && ObjectUtils.isEmpty(userPermissionPO.getAssignStaffFlag()) && ObjectUtils.isEmpty(userPermissionPO.getAssignPosFlag()) && ObjectUtils.isEmpty(userPermissionPO.getAssignPosFlag())){
                    userPermissionPO.setNoRestrictFlag(true);
                }
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        // 保存角色修改的相关权限数据
        saveOrUpdateBatch(unFlowPermission);

        List<Long> opIds = new ArrayList<>();
        //先删除关联数据
        List<Long> permissionIds = unFlowPermission.stream().map(UserPermissionPO::getId).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(permissionIds)) {
            QueryWrapper<UserPPositionPO> userPPositionPOQueryWrapper = new QueryWrapper<>();
            QueryWrapper<UserPDepartmentPO> userPDepartmentPOQueryWrapper = new QueryWrapper<>();
            QueryWrapper<UserPStaffPO> userPStaffPOQueryWrapper = new QueryWrapper<>();
            // sql超长问题处理
            int permissionBatch = permissionIds.size() / 1000;
            if (0 == permissionBatch) {
                userPPositionPOQueryWrapper.in(UserPPositionField.userpermissionId, permissionIds);
                userPDepartmentPOQueryWrapper.in(UserPDepartmentField.userpermissionId, permissionIds);
                userPStaffPOQueryWrapper.in(UserPStaffField.userpermissionId, permissionIds);
            } else {
                for (int i = 0; i < permissionBatch; i++) {
                    userPPositionPOQueryWrapper.or().in(UserPPositionField.userpermissionId, permissionIds.subList(i * 1000, i * 1000 + 1000));
                    userPDepartmentPOQueryWrapper.or().in(UserPDepartmentField.userpermissionId, permissionIds.subList(i * 1000, i * 1000 + 1000));
                    userPStaffPOQueryWrapper.or().in(UserPStaffField.userpermissionId, permissionIds.subList(i * 1000, i * 1000 + 1000));
                }
                if (permissionIds.size() % 1000 != 0) {
                    userPPositionPOQueryWrapper.or().in(UserPPositionField.userpermissionId, permissionIds.subList(permissionBatch * 1000, permissionIds.size()));
                    userPDepartmentPOQueryWrapper.or().in(UserPDepartmentField.userpermissionId, permissionIds.subList(permissionBatch * 1000, permissionIds.size()));
                    userPStaffPOQueryWrapper.or().in(UserPStaffField.userpermissionId, permissionIds.subList(permissionBatch * 1000, permissionIds.size()));
                }
            }

            userPPositionService.remove(userPPositionPOQueryWrapper);
            userPDepartmentService.remove(userPDepartmentPOQueryWrapper);
            userPStaffService.remove(userPStaffPOQueryWrapper);
        }
        List<UserPStaffPO> userPStaffPOS = new ArrayList<>();
        List<UserPDepartmentPO> userPDepartmentPOS = new ArrayList<>();
        List<UserPPositionPO> userPPositionPOS = new ArrayList<>();
        for (UserPermissionPO userPermissionPO : unFlowPermission) {
            opIds.add(userPermissionPO.getMenuOperateId());
            if (userPermissionPO.getAssignPosFlag() != null && userPermissionPO.getAssignPosFlag()) {
                if (!ObjectUtils.isEmpty(userPermissionPO.getPositions())) {
                    // 指定岗位数据
                    userPermissionPO.getPositions().forEach(userPPositionPO -> {
                        userPPositionPO.setIncludeLower(false);
                        userPPositionPO.setUserpermissionId(userPermissionPO.getId());
                    });
                    userPPositionPOS.addAll(userPermissionPO.getPositions());
                }else{
                    throw new PermissionException(PermissionErrorEnum.ASSIGN_ERROR);
                }
            }
            if (userPermissionPO.getAssignDeptFlag() != null && userPermissionPO.getAssignDeptFlag()) {
                if (!ObjectUtils.isEmpty(userPermissionPO.getDepartments())) {
                    // 指定部门数据
                    userPermissionPO.getDepartments().forEach(userPDepartmentPO -> {
                        userPDepartmentPO.setIncludeLower(false);
                        userPDepartmentPO.setUserpermissionId(userPermissionPO.getId());
                    });
                    userPDepartmentPOS.addAll(userPermissionPO.getDepartments());
                }else{
                    throw new PermissionException(PermissionErrorEnum.ASSIGN_ERROR);
                }
            }
            if (userPermissionPO.getAssignStaffFlag() != null && userPermissionPO.getAssignStaffFlag()) {
                if (!ObjectUtils.isEmpty(userPermissionPO.getStaffs())) {
                    // 指定人员数据
                    userPermissionPO.getStaffs().forEach(userPStaffPO -> userPStaffPO.setUserpermissionId(userPermissionPO.getId()));
                    userPStaffPOS.addAll(userPermissionPO.getStaffs());
                }else{
                    throw new PermissionException(PermissionErrorEnum.ASSIGN_ERROR);
                }
            }
        }
        modifyPosition(userPPositionPOS);
        modifyDepartment(userPDepartmentPOS);
        modifyStaff(userPStaffPOS);
        flowPermissionService.insertFlowPermissionByUserPermission(userPermissionPOList,operateMap);

    }

    /**
     * @description: 查询用户拥有的操作权限
     * @param: menuInfoCode
     * @return: java.util.List<java.lang.String>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<String> findUserOperate(String menuInfoCode) {
        QueryWrapper<UserPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mi.CODE", menuInfoCode);
        queryWrapper.eq("mo.VALID",1);
        queryWrapper.eq("USER_ID",UserContext.getUserContext().getUserId());
        return userPermissionMapper.findUserOperate(queryWrapper);
    }

    @Override
    public Boolean checkUserPermission(String menuOperateCode, String cid) {
        QueryWrapper<UserPermissionPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mo.VALID",1);
        queryWrapper.eq("mo.CODE",menuOperateCode);
        queryWrapper.eq("USER_ID",UserContext.getUserContext().getUserId());
        if (!ObjectUtils.isEmpty(cid)){
            queryWrapper.eq("mo.CID",cid);
        }
        List<String> userOperate = userPermissionMapper.findUserOperate(queryWrapper);
        return userOperate.size() > 0;
    }

    @Override
    public Boolean checkUserPermissionFusion(List<String> operateCodes, Long userId) {
        QueryWrapper<UserPermissionPO> queryWrapperUP = new QueryWrapper<>();
        queryWrapperUP.in(UserPermissionField.menuOperateCode,operateCodes);
        queryWrapperUP.eq(UserPermissionField.userId,userId);
        List<UserPermissionPO> list = this.list(queryWrapperUP);
        if (list.size() > 0){
            return true;
        }
        QueryWrapper<FlowPermissionPO> queryWrapperFP = new QueryWrapper<>();
        queryWrapperFP.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.USER);
        queryWrapperFP.in(FlowPermissionField.activityCode, operateCodes);
        queryWrapperFP.eq(FlowPermissionField.typeId,userId);
        List<FlowPermissionPO> list1 = flowPermissionService.list(queryWrapperFP);
        if (list.size() > 0){
            return true;
        }
        List<RoleUserPO> roleUserPOS = roleUserService.list(new QueryWrapper<RoleUserPO>().eq(RoleUserField.userId, userId));
        if (ObjectUtils.isEmpty(roleUserPOS)){
            return false;
        }
        List<Long> roleIds = roleUserPOS.stream().map(RoleUserPO::getRoleId).collect(Collectors.toList());
        QueryWrapper<FlowPermissionPO> queryWrapperFPRole = new QueryWrapper<>();
        queryWrapperFPRole.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE);
        queryWrapperFP.in(FlowPermissionField.activityCode, operateCodes);
        queryWrapperFPRole.in(FlowPermissionField.typeId,roleIds);
        List<FlowPermissionPO> listRole = flowPermissionService.list(queryWrapperFPRole);
        if (listRole.size() > 0){
            return true;
        }
        return false;
    }

    @Override
    public List<PrivilegeBO> findAllUserPermission(List<String> operateCodes, Long userId, Map<String,String> map) {
        List<PrivilegeBO> result = new ArrayList<>();
        if (ObjectUtils.isEmpty(operateCodes)){
            return result;
        }
        QueryWrapper<UserPermissionPO> queryWrapperUP = new QueryWrapper<>();
        queryWrapperUP.in(UserPermissionField.menuOperateCode,operateCodes);
        queryWrapperUP.eq(UserPermissionField.userId,userId);
        queryWrapperUP.eq(UserPermissionField.cid,UserContext.getUserContext().getCompanyId());
        List<UserPermissionPO> list = this.list(queryWrapperUP);
        list.forEach(userPermissionPO -> {
            PrivilegeBO p = new PrivilegeBO();
            p.setMenuCode(map.get(userPermissionPO.getMenuOperateCode()));
            p.setId(userPermissionPO.getId());
            p.setOperationCode(userPermissionPO.getMenuOperateCode());
            p.setUserId(userId);
            result.add(p);
        });
        QueryWrapper<FlowPermissionPO> queryWrapperFP = new QueryWrapper<>();
        queryWrapperFP.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.USER);
        queryWrapperFP.in(FlowPermissionField.activityCode, operateCodes);
        queryWrapperFP.eq(FlowPermissionField.typeId,userId);
        List<FlowPermissionPO> list1 = flowPermissionService.list(queryWrapperFP);
        list1.forEach(flowPermissionPO -> {
            PrivilegeBO p = new PrivilegeBO();
            p.setMenuCode(map.get(flowPermissionPO.getActivityCode()));
            p.setId(flowPermissionPO.getId());
            p.setOperationCode(flowPermissionPO.getActivityCode());
            p.setUserId(userId);
            result.add(p);
        });
        List<RoleUserPO> roleUserPOS = roleUserService.list(new QueryWrapper<RoleUserPO>().eq(RoleUserField.userId, userId));
        if (ObjectUtils.isEmpty(roleUserPOS)){
            return result;
        }
        List<Long> roleIds = roleUserPOS.stream().map(RoleUserPO::getRoleId).collect(Collectors.toList());
        QueryWrapper<FlowPermissionPO> queryWrapperFPRole = new QueryWrapper<>();
        queryWrapperFPRole.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE);
        queryWrapperFPRole.in(FlowPermissionField.activityCode, operateCodes);
        queryWrapperFPRole.in(FlowPermissionField.typeId,roleIds);
        List<FlowPermissionPO> listRole = flowPermissionService.list(queryWrapperFPRole);
        listRole.forEach(flowPermissionPO -> {
            PrivilegeBO p = new PrivilegeBO();
            p.setMenuCode(map.get(flowPermissionPO.getActivityCode()));
            p.setId(flowPermissionPO.getId());
            p.setOperationCode(flowPermissionPO.getActivityCode());
            p.setUserId(userId);
            result.add(p);
        });
        return result;
    }

    @Override
    public void cascadeDeleteUserPermission(Long cid, List<Long> opIds, Long rid, Long userId) {
        userPPositionMapper.deleteUserPPosition(cid, opIds, rid, userId);
        userPStaffMapper.deleteUserPStaff(cid, opIds, rid, userId);
        userCustomPermissionRefMapper.deleteUserCustomPermission(cid, opIds, rid, userId);
        userDataPermissionMapper.deleteUserDataPermission(cid, opIds, rid, userId);
        userPDepartmentMapper.deleteUserPDepartment(cid,opIds,rid,userId);
        userPermissionMapper.deleteUserPermission(cid, opIds, rid, userId);
    }

    /**
     * @description: 修改用户权限指定岗位
     * @param: staffs
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/9
     */
    private void modifyPosition(List<UserPPositionPO> positions) {
        if (CollectionUtils.isEmpty(positions)) {
            return;
        }
        // 解析岗位相关数据并插入用户岗位关联表
        userPPositionService.saveBatch(positions);
    }

    /**
     * @description: 修改用户权限指定部门
     * @param: staffs
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/9
     */
    private void modifyDepartment(List<UserPDepartmentPO> departments) {
        if (CollectionUtils.isEmpty(departments)) {
            return;
        }
        // 解析岗位相关数据并插入用户部门关联表
        userPDepartmentService.saveBatch(departments);
    }

    /**
     * @description: 修改用户权限指定人员
     * @param: staffs
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/9
     */
    private void modifyStaff(List<UserPStaffPO> staffs) {
        if (CollectionUtils.isEmpty(staffs)) {
            return;
        }
        // 解析人员相关数据并插入用户人员关联表
        userPStaffService.saveBatch(staffs);
    }
}
