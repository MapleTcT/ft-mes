package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.RoleCustomPermissionRefPO;
import com.supcon.supfusion.rbac.dao.po.RolePStaffPO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 自定义权限角色关联表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public interface RoleCustomPermissionRefMapper extends BaseMapper<RoleCustomPermissionRefPO> {

    @Select("SELECT * FROM rbac_role_custompermission_ref WHERE ROLEPERMISSION_ID = #{RolePermissionId}")
    List<RoleCustomPermissionRefPO> findByRolePermissionId(@Param("RolePermissionId") Long RolePermissionId);

}
