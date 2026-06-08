package com.supcon.supfusion.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import com.supcon.supfusion.auth.service.bo.UserBO;
import com.supcon.supfusion.auth.service.bo.UserRoleBO;

import java.util.List;

public interface UserRoleService extends IService<UserRolePO> {

    /**
     * 创建用户角色
     *
     * @param userRoleBOS
     */
    void batchInsert(List<UserRoleBO> userRoleBOS);

    void updateRoles(List<UserRoleBO> userRoleBOS,UserBO userBO);

    List<UserRoleBO> getRole(Long userId);

    List<UserBO> selectUserRole(Long roleId);

}
