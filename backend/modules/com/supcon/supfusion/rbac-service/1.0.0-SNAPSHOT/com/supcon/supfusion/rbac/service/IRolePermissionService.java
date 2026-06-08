package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.bo.RolePermissionBO;
import com.supcon.supfusion.rbac.dao.po.RolePermissionPO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface IRolePermissionService extends IService<RolePermissionPO> {

    /**
     * 根据条件获取授权列表
     *
     * @param menuId
     *            菜单ID
     * @param roleId
     *            角色ID
     * @param menuName
     *            菜单名称
     * @param operateName
     *            操作名称
     * @param local
     *            本地化信息
     * @return
     */
    List<RolePermissionBO> getRolePermissionList(Long roleId);

    /**
     * 为角色分配权限
     * @param rolePermissionPOList
     */
    void addOrUpdateRolePermission(List<RolePermissionPO> rolePermissionPOList);

    /**
     * @description: 刷新用户权限
     * @param: rid
     * @param: opIds
     * @param: userIds
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/17
     */
    void freshSubOperate(Long rid, List<Long> opIds, Long userId, Long cid);

    /**
     * 批量删除角色权限数据
     * @param rpList
     */
    void batchDeleteRolePermissions(List<RolePermissionPO> rpList);

    void grantPermission(Long roleId,List<String> menuOperateCodes,Long cid);
}
