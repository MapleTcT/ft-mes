package com.supcon.supfusion.rbac.service;

import com.supcon.supfusion.rbac.dao.po.RbacDataResourceGroupPO;
import com.supcon.supfusion.rbac.dao.po.RbacRoleDataPermissionPO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionPO;
import com.supcon.supfusion.rbac.service.bo.RoleDataResourceResponseBO;
import com.supcon.supfusion.rbac.service.bo.UserDataResourceResponseBO;

import java.util.List;

public interface DataPermissionService {
    List<RbacDataResourceGroupPO> getDataResourceGroups(Long cid);

    void saveDataResourceForUser(Long userId, Long cid, String groupCode, boolean controlled, List<RbacUserDataPermissionPO> rbacUserDataResourcePermissionPOS);

    UserDataResourceResponseBO queryDataResourceByUser(Long userId, Long cid, String groupCode);

    void saveDataResourceForRole(Long roleId, Long cid, String groupCode, boolean controlled, List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS);

    RoleDataResourceResponseBO queryDataResourceByRole(Long roleId, Long cid, String groupCode);

    void bindUserPermissionByRole(Long roleId, Long userId);

    void unbindUserPermissionByRole(Long roleId, Long userId);

    void emptyUserPermissionFromRole(Long userId);

    void emptyUserPermissionFromRoleByCid(Long userId, Long cid);

    void emptyUserPermission(Long userId);

}
