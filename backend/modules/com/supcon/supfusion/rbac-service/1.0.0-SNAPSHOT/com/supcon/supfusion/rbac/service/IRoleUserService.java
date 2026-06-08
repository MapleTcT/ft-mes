package com.supcon.supfusion.rbac.service;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.bo.UserDetailBO;

import java.util.List;

/**
 * <p>
 * 角色用户表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface IRoleUserService extends IService<RoleUserPO> {

    void saveRoleUsers(Long roleUserId, List<UserDetailBO> userDetailBOS);

    void deleteRoleUsers(String  roleUserId);

    PageResult<RoleUserPO> findByPage(String roleCode,String keyword, Integer current, Integer pageSize,Long cid);

    void createTemp(List<Long> roleUserIds, String id, Long roleId,String keyword);

    void createTemp(int current,int pageSize, String id, Long roleId,String keyword);

    void export(String id);

    List<RoleUserPO> getAllRoleUser();
}
