package com.supcon.supfusion.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.rbac.dao.po.UserPermissionPO;
import com.supcon.supfusion.rbac.service.bo.PrivilegeBO;
import com.supcon.supfusion.rbac.dao.bo.UserPermissionBO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface IUserPermissionService extends IService<UserPermissionPO> {

    /**
     * @description: 获取用户所有权限
     * @param: userId
     * @param: menuName
     * @param: operateName
     * @param: local
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.bo.UserPermissionBO>
     * @author: 袁阳
     * @date: 2020/6/18
     */
    List<UserPermissionBO> getUserPermissionListFull(Long userId,Integer purviewType);

    /**
     * 批量删除用户权限数据
     * @param upList
     */
    void batchDeleteUserPermissions(List<UserPermissionPO> upList);

    /**
     * 为用户分配权限
     * @param userPermissionPOList
     */
    void addOrUpdateUserPermission(List<UserPermissionPO> userPermissionPOList);

    /**
     * 判断该菜单有哪些操作权限
     * @param menuInfoCode
     */
    List<String> findUserOperate(String menuInfoCode);

    /**
     * 判断当前用户是否有该操作权限
     * @param menuOperateCode
     * @param cid
     * @return
     */
    Boolean checkUserPermission(String menuOperateCode,String cid);

    /**
     * 判断当前用户是否有该操作权限
     *
     * @param operateCodes
     * @param userId
     * @return
     */
    Boolean checkUserPermissionFusion(List<String> operateCodes,Long userId);

    List<PrivilegeBO> findAllUserPermission(List<String> operateCodes, Long userId, Map<String,String> map);

    /**
     * 级联删用户权限
     */
    void cascadeDeleteUserPermission(Long cid,List<Long> opIds,Long rid,Long userId);
}
