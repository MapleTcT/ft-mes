package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.rbac.dao.po.RolePO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface IRoleService extends IService<RolePO> {

    void deleteRoles(String codes);

    void deleteRolesByArray(List<String> codes);

    List<Map<String, Object>> getRoleTree(String keyword, String tag);

    List<Map<String, Object>> getRoleTreeSingleCompany(String keyword, String tag,Long cid);

    void saveRole(RolePO role, List<String> tags);

    void update(RolePO role, List<Long> deleteIds, List<String> tags);

    List<RolePO> getRoleByUserPermission(Long menuOperateId, Long userId);

    List<RolePO> getRoleByUserPermissionFlow(String menuOperateCode, List<Long> roleIds);

    List<RolePO> getRoleTreeNoTag(String keyword,Long cid);

    PageResult<RolePO> getRolesByPage(Integer current, Integer pageSize);

    PageResult<RolePO> querySubRolesByParentCode(String roleCode, Boolean all, Integer current, Integer pageSize);
}
