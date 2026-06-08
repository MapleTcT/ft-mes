package com.supcon.supfusion.rbac.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.rbac.api.IRoleUserApiService;
import com.supcon.supfusion.rbac.api.dto.*;
import com.supcon.supfusion.rbac.dao.RoleMapper;
import com.supcon.supfusion.rbac.dao.RoleUserMapper;
import com.supcon.supfusion.rbac.dao.field.UserPermissionField;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import com.supcon.supfusion.rbac.common.utils.ThreadPoolUtils;
import java.util.*;
import java.util.stream.Collectors;

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
public class RoleUserApiServiceImpl implements IRoleUserApiService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleUserMapper roleUserMapper;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private IUserPermissionService userPermissionService;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Autowired
    private IUserPPositionService userPPositionService;
    @Autowired
    private IUserPStaffService userPStaffService;
    @Autowired
    private IUserCustomPermissionRefService userCustomPermissionRefService;
    @Autowired
    private IUserDataPermissionService userDataPermissionService;
    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private DataPermissionService dataPermissionService;


    @Override
    public List<RoleDTO> findRoleUserByUserId(Long id, Long cid) {
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("USER_ID", id);
        queryWrapper.eq("role.CID", cid);
        queryWrapper.eq("ru.VALID", 1);
        List<RolePO> roleUserByUserId = roleMapper.findRoleUserByUserId(queryWrapper);
        return roleUserByUserId.stream().map(rolePO -> {
            RoleDTO roleDTO = new RoleDTO();
            BeanUtils.copyProperties(rolePO, roleDTO);
            return roleDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RoleUserDTO> findUserByRoleId(List<Long> ids) {
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ROLE_ID", ids);
        List<RoleUserPO> roleUserPOS = roleUserMapper.selectList(queryWrapper);
        return roleUserPOS.stream().map(roleUserPO -> {
            RoleUserDTO roleUserDTO = new RoleUserDTO();
            BeanUtils.copyProperties(roleUserPO, roleUserDTO);
            return roleUserDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RoleUserDTO> findUserByRoleCode(List<String> codes) {
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("role.CODE", codes);
        queryWrapper.in("role.VALID", 1);
        List<RoleUserPO> roleUserPOS = roleUserMapper.findUserByRoleCode(queryWrapper);
        return roleUserPOS.stream().map(roleUserPO -> {
            RoleUserDTO roleUserDTO = new RoleUserDTO();
            BeanUtils.copyProperties(roleUserPO, roleUserDTO);
            return roleUserDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveRoleUser(RoleUserDTO roleUserDTO) {
        RoleUserPO roleUserPO = new RoleUserPO();
        BeanUtils.copyProperties(roleUserDTO, roleUserPO);
        roleUserService.saveOrUpdate(roleUserPO);
        rolePermissionService.freshSubOperate(roleUserDTO.getRoleId(), null, null, null);

        /**
         * 数据权限
         */
        dataPermissionService.bindUserPermissionByRole(roleUserDTO.getRoleId(), roleUserDTO.getUserId());
    }

    @Override
    public void batchSaveOneUser(RoleUserAddBatchDTO roleUserAddBatchDTO) {
        if (!ObjectUtils.isEmpty(roleUserAddBatchDTO.getRoleIds()) && !ObjectUtils.isEmpty(roleUserAddBatchDTO.getUserId())) {
            List<RoleUserPO> roleUserPOS = roleUserAddBatchDTO.getRoleIds().stream().map(roleId -> {
                RoleUserPO roleUserPO = new RoleUserPO();
                roleUserPO.setRoleId(roleId);
                List<RoleUserPO> roleUserPOList = roleUserService.list(new QueryWrapper<RoleUserPO>().eq("ROLE_ID", roleId).eq("USER_ID", roleUserAddBatchDTO.getUserId()));
                if (!ObjectUtils.isEmpty(roleUserPOList)) {
                    return null;
                }
                BeanUtils.copyProperties(roleUserAddBatchDTO, roleUserPO);
                return roleUserPO;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            roleUserService.saveOrUpdateBatch(roleUserPOS);
            ThreadPoolUtils.getThreadPool().execute(() -> {
                roleUserPOS.forEach(roleUserPO -> rolePermissionService.freshSubOperate(roleUserPO.getRoleId(), null, null, null));
                if (!ObjectUtils.isEmpty(roleUserPOS)){
                    userUrlRefService.addUserUrlRefListForUserFlow(roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList()),null, RpcContext.getContext().getTenantId());
                }
                /**
                 * 数据权限
                 */
                roleUserAddBatchDTO.getRoleIds().forEach(roleId -> {
                    dataPermissionService.bindUserPermissionByRole(roleId, roleUserAddBatchDTO.getUserId());
                });
            });
        }
    }


    @Override
    public void batchSaveRoleUser(List<RoleUserDTO> roleUserDTOS) {
        if (!ObjectUtils.isEmpty(roleUserDTOS)) {
            List<RoleUserPO> roleUserPOS = roleUserDTOS.stream().map(roleUserDTO -> {
                RoleUserPO roleUserPO = new RoleUserPO();
                BeanUtils.copyProperties(roleUserDTO, roleUserPO);
                return roleUserPO;
            }).collect(Collectors.toList());
            roleUserService.saveOrUpdateBatch(roleUserPOS);
            ThreadPoolUtils.getThreadPool().execute(() -> {
                roleUserPOS.forEach(roleUserPO -> rolePermissionService.freshSubOperate(roleUserPO.getRoleId(), null, null, null));
                if (!ObjectUtils.isEmpty(roleUserPOS)){
                    userUrlRefService.addUserUrlRefListForUserFlow(roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList()),null, RpcContext.getContext().getTenantId());
                }
                /**
                 * 数据权限
                 */
                roleUserDTOS.forEach(roleUserDTO -> {
                    dataPermissionService.bindUserPermissionByRole(roleUserDTO.getRoleId(), roleUserDTO.getUserId());
                });
            });
        }
    }

    @Override
    public void updateRoleUser(RoleUserAddBatchDTO roleUserAddBatchDTO) {
        //先清空该用户的角色关联 再重新关联角色
        Long userId = roleUserAddBatchDTO.getUserId();
        if (!ObjectUtils.isEmpty(userId)) {
            roleUserService.remove(new QueryWrapper<RoleUserPO>().eq("USER_ID", userId));
            List<Long> roleIds = roleUserAddBatchDTO.getRoleIds();
            if (!ObjectUtils.isEmpty(roleIds)) {
                List<RoleUserPO> roleUserPOS = roleIds.stream().map(id -> {
                    RoleUserPO roleUserPO = new RoleUserPO();
                    roleUserPO.setRoleId(id);
                    roleUserPO.setUserName(roleUserAddBatchDTO.getUserName());
                    roleUserPO.setUserId(roleUserAddBatchDTO.getUserId());
                    roleUserPO.setPersonCode(roleUserAddBatchDTO.getPersonCode());
                    roleUserPO.setPersonName(roleUserAddBatchDTO.getPersonName());
                    return roleUserPO;
                }).collect(Collectors.toList());
                roleUserService.saveBatch(roleUserPOS);
                ThreadPoolUtils.getThreadPool().execute(() -> {
                    roleUserPOS.forEach(roleUserPO -> rolePermissionService.freshSubOperate(roleUserPO.getRoleId(), null, null, null));
                    if (!ObjectUtils.isEmpty(roleUserPOS)){
                        userUrlRefService.addUserUrlRefListForUserFlow(roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList()),null, RpcContext.getContext().getTenantId());
                    }
                    /**
                     * 数据权限
                     *
                     *
                     * 这里的roleIds是不区分cid的全量角色id,所以如果为空说明所有公司的角色都被解绑
                     */
                    dataPermissionService.emptyUserPermissionFromRole(roleUserAddBatchDTO.getUserId());
                    roleUserAddBatchDTO.getRoleIds().forEach(roleId -> {
                        dataPermissionService.bindUserPermissionByRole(roleId, roleUserAddBatchDTO.getUserId());
                    });
                });
            } else {
                userPermissionService.remove(new QueryWrapper<UserPermissionPO>().eq("USER_ID", userId).eq("cid", UserContext.getUserContext().getCompanyId()).eq("POSITION_FLAG", 0));
                userUrlRefService.addUserUrlRefListForUserFlow(Collections.singletonList(userId), null, RpcContext.getContext().getTenantId());
                /**
                 * 这里的roleIds是不区分cid的全量角色id,所以如果为空说明所有公司的角色都被解绑
                 */
                dataPermissionService.emptyUserPermissionFromRole(roleUserAddBatchDTO.getUserId());
            }
        }
    }

    @Override
    public void deleteByUserId(List<Long> userId) {
        //删除角色用户关联
        roleUserService.remove(new QueryWrapper<RoleUserPO>().in("USER_ID", userId));
        //删除用户权限
        List<UserPermissionPO> user_id = userPermissionService.list(new QueryWrapper<UserPermissionPO>().in("USER_ID", userId));
        userPermissionService.remove(new QueryWrapper<UserPermissionPO>().in("USER_ID", userId));
        if (!ObjectUtils.isEmpty(user_id)) {
            //删除用户权限岗位关联
            userPPositionService.remove(new QueryWrapper<UserPPositionPO>().in("USERPERMISSION_ID", user_id.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
            userPStaffService.remove(new QueryWrapper<UserPStaffPO>().in("USERPERMISSION_ID", user_id.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
            userCustomPermissionRefService.remove(new QueryWrapper<UserCustomPermissionRefPO>().in("USERPERMISSION_ID", user_id.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
            userDataPermissionService.remove(new QueryWrapper<UserDataPermissionPO>().in("USERPERMISSION_ID", user_id.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
        }
        /**
         * 数据权限
         */
        userId.forEach(id -> {
            dataPermissionService.emptyUserPermission(id);
        });
    }

    @Override
    public void batchSaveOneUserFR(List<RoleUserFRDTO> roleUserFRDTOs) {
        //先清空该用户的角色关联 再重新关联角色
        if (ObjectUtils.isEmpty(roleUserFRDTOs)) {
            return;
        }
        roleUserFRDTOs.forEach(roleUserFRDTO -> {
            Long userId = roleUserFRDTO.getUserId();
            if (!ObjectUtils.isEmpty(userId)) {
                roleUserService.remove(new QueryWrapper<RoleUserPO>().eq("USER_ID", userId));
                List<RoleFRDTO> roles = roleUserFRDTO.getRoles();
                if (!ObjectUtils.isEmpty(roles)) {
                    List<RoleUserPO> roleUserPOS = roles.stream().map(roleFRDTO -> {
                        RoleUserPO roleUserPO = new RoleUserPO();
                        roleUserPO.setRoleId(roleFRDTO.getId());
                        roleUserPO.setUserName(roleUserFRDTO.getUserName());
                        roleUserPO.setUserId(roleUserFRDTO.getUserId());
                        roleUserPO.setPersonCode(roleUserFRDTO.getPersonCode());
                        roleUserPO.setPersonName(roleUserFRDTO.getPersonName());
                        roleUserPO.setFromPosition(roleFRDTO.getFromPosition());
                        //仅来自于岗位
                        if (roleFRDTO.getFromPosition() == 2) {
                            roleUserPO.setPositionFlag(true);
                        } else {
                            roleUserPO.setPositionFlag(false);
                        }
                        return roleUserPO;
                    }).collect(Collectors.toList());
                    roleUserService.saveBatch(roleUserPOS);
                    if (!ObjectUtils.isEmpty(roleUserPOS)) {
                        ThreadPoolUtils.getThreadPool().execute(() -> {
                            //去重
                            List<Long> roleIds = roleUserPOS.stream().map(RoleUserPO::getRoleId).distinct().collect(Collectors.toList());
                            roleIds.forEach(roleId -> rolePermissionService.freshSubOperate(roleId, null, userId, roleUserFRDTO.getCid()));
                            userUrlRefService.addUserUrlRefListForUserFlow(roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList()), null, RpcContext.getContext().getTenantId());
                            /**
                             * 数据权限
                             */
                            dataPermissionService.emptyUserPermissionFromRoleByCid(userId, roleUserFRDTO.getCid());
                            roleUserFRDTO.getRoles().forEach(role -> {
                                dataPermissionService.bindUserPermissionByRole(role.getId(), userId);
                            });
                        });
                    }
                } else {
                    userPermissionService.remove(new QueryWrapper<UserPermissionPO>().eq(UserPermissionField.userId, userId).eq(UserPermissionField.purviewType,0));
                    userUrlRefService.addUserUrlRefListForUserFlow(Collections.singletonList(userId),roleUserFRDTO.getCid(), RpcContext.getContext().getTenantId());
                    /**
                     * 数据权限
                     */
                    dataPermissionService.emptyUserPermissionFromRoleByCid(userId, roleUserFRDTO.getCid());
                }
            }
        });
    }
}
