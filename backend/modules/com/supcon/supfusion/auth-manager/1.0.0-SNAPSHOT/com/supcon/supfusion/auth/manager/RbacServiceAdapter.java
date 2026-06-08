package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.rbac.api.dto.*;

import java.util.List;
import java.util.Map;

public interface RbacServiceAdapter {

    Map<Long, String> findBatchName(String ids);

    List<RoleDTO> findRoleByCodes(List<String> codes);

    RoleDTO createAdminRole(AdminRoleDTO adminRoleDTO);

    void batchUpdateOneUser(RoleUserAddBatchDTO roleUserAddBatchDTO);

    void deleteRolesByUserIds(List<Long> userIds);

    RoleDTO bindCompanyAdminUser(AdminRoleDTO adminRoleDTO);

    void batchSaveOneUserFR(List<RoleUserFRDTO> roleUserFRDTOS);

    List<RoleDTO> findRoleByIds(List<Long> ids);

    PageResult<RoleDTO> getRolesByCid(Long cid, String keyword, Integer current, Integer pageSize);


    void addRole(Long cid,String code,String name,String description);

    void updateRole(String code,String name,String description);

    void deleteRoles(List<String> codes);

    RoleResourceDTO getRoleDetail(String code);


}
