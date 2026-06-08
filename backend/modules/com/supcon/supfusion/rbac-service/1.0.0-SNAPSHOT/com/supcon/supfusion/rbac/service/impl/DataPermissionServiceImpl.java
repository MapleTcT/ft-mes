package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.rbac.dao.RoleMapper;
import com.supcon.supfusion.rbac.dao.RoleUserMapper;
import com.supcon.supfusion.rbac.dao.po.RbacDataResourceGroupPO;
import com.supcon.supfusion.rbac.dao.po.RbacRoleDataPermissionCtrlPO;
import com.supcon.supfusion.rbac.dao.po.RbacRoleDataPermissionPO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionCtrlPO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionPO;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.DataPermissionService;
import com.supcon.supfusion.rbac.service.bo.RoleDataResourceResponseBO;
import com.supcon.supfusion.rbac.service.bo.UserDataResourceResponseBO;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacDataResourceGroupServiceImpl;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacRoleDataPermissionCtrlServiceImpl;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacRoleDataPermissionServiceImpl;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacUserDataPermissionCtrlServiceImpl;
import com.supcon.supfusion.rbac.service.impl.mybatisplus.RbacUserDataPermissionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户直接分配
 * 用户清空角色
 * 用户删除
 * 角色直接分配
 * 角色绑定用户
 * 角色解绑用户
 * 角色删除
 * 角色绑定岗位
 * 角色解绑岗位
 * 岗位删除
 */

@Slf4j
@Service
public class DataPermissionServiceImpl implements DataPermissionService {
    @Autowired
    private RbacDataResourceGroupServiceImpl rbacDataResourceGroupService;
    @Autowired
    private RbacUserDataPermissionServiceImpl rbacUserDataPermissionService;
    @Autowired
    private RbacUserDataPermissionCtrlServiceImpl rbacUserDataPermissionCtrlService;
    @Autowired
    private RbacRoleDataPermissionServiceImpl rbacRoleDataPermissionService;
    @Autowired
    private RbacRoleDataPermissionCtrlServiceImpl rbacRoleDataPermissionCtrlService;
    @Autowired
    private RoleUserMapper roleUserMapper;
    @Autowired
    private RoleMapper roleMapper;

    @Override
    public List<RbacDataResourceGroupPO> getDataResourceGroups(Long cid) {
        return rbacDataResourceGroupService.list(Wrappers.<RbacDataResourceGroupPO>query().eq(RbacDataResourceGroupPO.getCidFieldName(), cid).or().isNull(RbacDataResourceGroupPO.getCidFieldName()));
    }

    /**
     * 用户直接分配
     *
     * @param userId
     * @param cid
     * @param groupCode
     * @param controlled
     * @param rbacUserDataResourcePermissionPOS
     */
    @Override
    @Transactional
    public void saveDataResourceForUser(Long userId, Long cid, String groupCode, boolean controlled, List<RbacUserDataPermissionPO> rbacUserDataResourcePermissionPOS) {

        if (controlled) {
            updateRbacUserDataPermissionCtrl(cid, userId, groupCode, 1);
            rbacUserDataPermissionService.remove(Wrappers.<RbacUserDataPermissionPO>query()
                    .eq(RbacUserDataPermissionPO.getUserIdFieldName(), userId)
                    .eq(RbacUserDataPermissionPO.getCidFieldName(), cid)
                    .eq(RbacUserDataPermissionPO.getPurviewTypeFieldName(), 1)
                    .eq(RbacUserDataPermissionPO.getGroupCodeFieldName(), groupCode));
            rbacUserDataPermissionService.saveBatch(rbacUserDataResourcePermissionPOS);
        } else {
            updateRbacUserDataPermissionCtrl(cid, userId, groupCode, 0);
            rbacUserDataPermissionService.remove(Wrappers.<RbacUserDataPermissionPO>query()
                    .eq(RbacUserDataPermissionPO.getUserIdFieldName(), userId)
                    .eq(RbacUserDataPermissionPO.getCidFieldName(), cid)
                    .eq(RbacUserDataPermissionPO.getPurviewTypeFieldName(), 1)
                    .eq(RbacUserDataPermissionPO.getGroupCodeFieldName(), groupCode));
        }
    }

    private void updateRbacUserDataPermissionCtrl(Long cid, Long userId, String groupCode, int controlled) {
        RbacUserDataPermissionCtrlPO rbacUserDataPermissionCtrlPO = new RbacUserDataPermissionCtrlPO();
        rbacUserDataPermissionCtrlPO.setId(IDGenerator.newInstance().generate().longValue());
        rbacUserDataPermissionCtrlPO.setCid(cid);
        rbacUserDataPermissionCtrlPO.setUserId(userId);
        rbacUserDataPermissionCtrlPO.setGroupCode(groupCode);
        rbacUserDataPermissionCtrlPO.setControlled(controlled);
        rbacUserDataPermissionCtrlService.saveOrUpdate(rbacUserDataPermissionCtrlPO,
                Wrappers.<RbacUserDataPermissionCtrlPO>query()
                        .eq(RbacUserDataPermissionCtrlPO.getUserIdFieldName(), userId)
                        .eq(RbacUserDataPermissionCtrlPO.getCidFieldName(), cid)
                        .eq(RbacUserDataPermissionCtrlPO.getGroupCodeFieldName(), groupCode));
    }

    @Override
    public UserDataResourceResponseBO queryDataResourceByUser(Long userId, Long cid, String groupCode) {
        UserDataResourceResponseBO userDataResourceResponseBO = new UserDataResourceResponseBO();

        RbacUserDataPermissionCtrlPO rbacUserDataPermissionCtrlPO = rbacUserDataPermissionCtrlService.getOne(Wrappers.<RbacUserDataPermissionCtrlPO>query()
                .eq(RbacUserDataPermissionCtrlPO.getUserIdFieldName(), userId)
                .eq(RbacUserDataPermissionCtrlPO.getCidFieldName(), cid)
                .eq(RbacUserDataPermissionCtrlPO.getGroupCodeFieldName(), groupCode));
        if (rbacUserDataPermissionCtrlPO != null && rbacUserDataPermissionCtrlPO.getControlled() == 0) {
            userDataResourceResponseBO.setControlled(false);
            return userDataResourceResponseBO;
        }

        userDataResourceResponseBO.setControlled(true);
        List<RbacUserDataPermissionPO> rbacUserDataPermissionPOS = rbacUserDataPermissionService.list(Wrappers.<RbacUserDataPermissionPO>query()
                .eq(RbacUserDataPermissionCtrlPO.getUserIdFieldName(), userId)
                .eq(RbacUserDataPermissionCtrlPO.getCidFieldName(), cid)
                .eq(RbacUserDataPermissionCtrlPO.getGroupCodeFieldName(), groupCode)
                .eq(RbacUserDataPermissionPO.getPurviewTypeFieldName(), 1));
        userDataResourceResponseBO.setDataResouceVOS(rbacUserDataPermissionPOS);
        return userDataResourceResponseBO;
    }

    /**
     * 角色直接分配
     *
     * @param roleId
     * @param cid
     * @param groupCode
     * @param controlled
     * @param rbacRoleDataPermissionPOS
     */
    @Override
    @Transactional
    public void saveDataResourceForRole(Long roleId, Long cid, String groupCode, boolean controlled, List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS) {
        if (controlled) {
            updateRbacRoleDataPermissionCtrl(cid, roleId, groupCode, 1);
            deleteRolePermissionByRoleId(roleId, cid, groupCode);
            deleteUserPermissionByRoleId(roleId, cid, groupCode);

            rbacRoleDataPermissionService.saveBatch(rbacRoleDataPermissionPOS);
            List<RoleUserPO> roleUserPOS = roleUserMapper.selectList(Wrappers.<RoleUserPO>query().eq(RoleUserPO.getRoleIdFeildName(), roleId).eq(RoleUserPO.getValidFeildName(), 1));
            if (roleUserPOS != null) {
                for (RoleUserPO roleUserPO : roleUserPOS) {
                    addUserPermissionByRole(roleUserPO.getUserId(), cid, roleId, rbacRoleDataPermissionPOS);
                }
            }
        } else {
            updateRbacRoleDataPermissionCtrl(cid, roleId, groupCode, 0);
            deleteRolePermissionByRoleId(roleId, cid, groupCode);
            deleteUserPermissionByRoleId(roleId, cid, groupCode);
        }
    }

    @Override
    public RoleDataResourceResponseBO queryDataResourceByRole(Long roleId, Long cid, String groupCode) {
        RoleDataResourceResponseBO roleDataResourceResponseBO = new RoleDataResourceResponseBO();

        RbacRoleDataPermissionCtrlPO rbacRoleDataPermissionCtrlPO = rbacRoleDataPermissionCtrlService.getOne(Wrappers.<RbacRoleDataPermissionCtrlPO>query()
                .eq(RbacRoleDataPermissionCtrlPO.getRoleIdFieldName(), roleId)
                .eq(RbacRoleDataPermissionCtrlPO.getCidFieldName(), cid)
                .eq(RbacRoleDataPermissionCtrlPO.getGroupCodeFieldName(), groupCode));
        if (rbacRoleDataPermissionCtrlPO != null && rbacRoleDataPermissionCtrlPO.getControlled() == 0) {
            roleDataResourceResponseBO.setControlled(false);
            return roleDataResourceResponseBO;
        }

        roleDataResourceResponseBO.setControlled(true);
        List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS = rbacRoleDataPermissionService.list(Wrappers.<RbacRoleDataPermissionPO>query()
                .eq(RbacRoleDataPermissionPO.getRoleIdFieldName(), roleId)
                .eq(RbacRoleDataPermissionPO.getCidFieldName(), cid)
                .eq(RbacRoleDataPermissionPO.getGroupCodeFieldName(), groupCode));
        roleDataResourceResponseBO.setDataResouceVOS(rbacRoleDataPermissionPOS);
        return roleDataResourceResponseBO;
    }

    /**
     * 角色绑定用户
     *
     * @param roleId
     * @param userId
     */
    @Override
    public void bindUserPermissionByRole(Long roleId, Long userId) {
        List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS = rbacRoleDataPermissionService.list(Wrappers.<RbacRoleDataPermissionPO>query().eq(RbacRoleDataPermissionPO.getRoleIdFieldName(), roleId));
        if (rbacRoleDataPermissionPOS == null || rbacRoleDataPermissionPOS.size() == 0) {
            return;
        }
        RolePO rolePO = roleMapper.selectOne(Wrappers.<RolePO>query().eq("id", roleId));
        deleteUserPermissionByRoleId(roleId, rolePO.getCid(), null);
        addUserPermissionByRole(userId, rolePO.getCid(), roleId, rbacRoleDataPermissionPOS);
    }

    /**
     * 角色解绑用户
     *
     * @param roleId
     * @param userId
     */
    @Override
    public void unbindUserPermissionByRole(Long roleId, Long userId) {
        List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS = rbacRoleDataPermissionService.list(Wrappers.<RbacRoleDataPermissionPO>query().eq(RbacRoleDataPermissionPO.getRoleIdFieldName(), roleId));
        if (rbacRoleDataPermissionPOS == null || rbacRoleDataPermissionPOS.size() == 0) {
            return;
        }
        RolePO rolePO = roleMapper.selectOne(Wrappers.<RolePO>query().eq("id", roleId));
        deleteUserPermissionByRoleId(roleId, rolePO.getCid(), null);
    }

    /**
     * 用户清空角色
     *
     * @param userId
     */
    @Override
    public void emptyUserPermissionFromRole(Long userId) {
        rbacUserDataPermissionService.remove(Wrappers.<RbacUserDataPermissionPO>query().eq(RbacUserDataPermissionPO.getUserIdFieldName(), userId).eq(RbacUserDataPermissionPO.getPurviewTypeFieldName(), 0));
    }

    /**
     * 用户清空指定公司角色
     *
     * @param userId
     */
    @Override
    public void emptyUserPermissionFromRoleByCid(Long userId, Long cid) {
        rbacUserDataPermissionService.remove(Wrappers.<RbacUserDataPermissionPO>query().eq(RbacUserDataPermissionPO.getUserIdFieldName(), userId).eq(RbacUserDataPermissionPO.getPurviewTypeFieldName(), 0).eq(RbacUserDataPermissionPO.getCidFieldName(), cid));
    }

    /**
     * 用户删除
     *
     * @param userId
     */
    @Override
    public void emptyUserPermission(Long userId) {
        rbacUserDataPermissionService.remove(Wrappers.<RbacUserDataPermissionPO>query().eq(RbacUserDataPermissionPO.getUserIdFieldName(), userId));
    }


    private void updateRbacRoleDataPermissionCtrl(Long cid, Long roleId, String groupCode, int controlled) {
        RbacRoleDataPermissionCtrlPO rbacRoleDataPermissionCtrlPO = new RbacRoleDataPermissionCtrlPO();
        rbacRoleDataPermissionCtrlPO.setId(IDGenerator.newInstance().generate().longValue());
        rbacRoleDataPermissionCtrlPO.setCid(cid);
        rbacRoleDataPermissionCtrlPO.setRoleId(roleId);
        rbacRoleDataPermissionCtrlPO.setGroupCode(groupCode);
        rbacRoleDataPermissionCtrlPO.setControlled(controlled);
        rbacRoleDataPermissionCtrlService.saveOrUpdate(rbacRoleDataPermissionCtrlPO,
                Wrappers.<RbacRoleDataPermissionCtrlPO>query()
                        .eq(RbacRoleDataPermissionCtrlPO.getRoleIdFieldName(), roleId)
                        .eq(RbacRoleDataPermissionCtrlPO.getCidFieldName(), cid)
                        .eq(RbacRoleDataPermissionCtrlPO.getGroupCodeFieldName(), groupCode));
    }

    private void deleteRolePermissionByRoleId(Long roleId, Long cid, String groupCode) {
        rbacRoleDataPermissionService.remove(Wrappers.<RbacRoleDataPermissionPO>query()
                .eq(RbacRoleDataPermissionPO.getRoleIdFieldName(), roleId)
                .eq(RbacRoleDataPermissionPO.getCidFieldName(), cid)
                .eq(RbacRoleDataPermissionPO.getGroupCodeFieldName(), groupCode));
    }

    private void deleteUserPermissionByRoleId(Long roleId, Long cid, String groupCode) {
        QueryWrapper queryWrapper = Wrappers.<RbacUserDataPermissionPO>query()
                .eq(RbacUserDataPermissionPO.getRoleIdFieldName(), roleId)
                .eq(RbacUserDataPermissionPO.getCidFieldName(), cid)
                .eq(RbacUserDataPermissionPO.getPurviewTypeFieldName(), 0);

        if (!StringUtils.isBlank(groupCode)) {
            queryWrapper.eq(RbacUserDataPermissionPO.getGroupCodeFieldName(), groupCode);
        }
        rbacUserDataPermissionService.remove(queryWrapper);
    }

    private void addUserPermissionByRole(Long userId, Long cid, Long roleId, List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS) {
        List<RbacUserDataPermissionPO> rbacUserDataPermissionPOS = new ArrayList<>();
        for (RbacRoleDataPermissionPO rbacRoleDataPermissionPO : rbacRoleDataPermissionPOS) {
            RbacUserDataPermissionPO rbacUserDataPermissionPO = new RbacUserDataPermissionPO();
            rbacUserDataPermissionPO.setId(IDGenerator.newInstance().generate().longValue());
            rbacUserDataPermissionPO.setPurviewType(0);
            rbacUserDataPermissionPO.setRoleId(roleId);
            rbacUserDataPermissionPO.setCid(cid);
            rbacUserDataPermissionPO.setGroupCode(rbacRoleDataPermissionPO.getGroupCode());
            rbacUserDataPermissionPO.setUserId(userId);
            rbacUserDataPermissionPO.setResourceCode(rbacRoleDataPermissionPO.getResourceCode());
            rbacUserDataPermissionPO.setResourceName(rbacRoleDataPermissionPO.getResourceName());
            rbacUserDataPermissionPO.setResourceType(rbacRoleDataPermissionPO.getResourceType());
            rbacUserDataPermissionPOS.add(rbacUserDataPermissionPO);
        }
        rbacUserDataPermissionService.saveBatch(rbacUserDataPermissionPOS);
    }


}
