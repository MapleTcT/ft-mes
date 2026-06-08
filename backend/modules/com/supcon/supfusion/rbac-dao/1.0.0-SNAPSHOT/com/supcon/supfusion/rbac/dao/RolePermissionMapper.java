package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.rbac.dao.bo.RolePermissionBO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.RolePermissionPO;
import com.supcon.supfusion.rbac.dao.provider.RolePermissionProvider;
import com.supcon.supfusion.rbac.dao.query.RolePermissionQuery;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface RolePermissionMapper extends BaseMapper<RolePermissionPO> {


    List<RolePermissionBO> getRolePermissionList(RolePermissionQuery rolePermissionQuery);

    List<MenuInfoPO> getRoleMenus(@Param("roleId") Long roleId);

    @SelectProvider(value = RolePermissionProvider.class,method = "getNewPermissionList")
    @Results({
            @Result(column = "USER_ID",property = "USER_ID",javaType = Long.class),
            @Result(column = "DEALER_PERMISSION_FLAG",property = "DEALER_PERMISSION_FLAG",javaType = Integer.class),
            @Result(column = "NO_RESTRICT_FLAG",property = "NO_RESTRICT_FLAG",javaType = Integer.class),
            @Result(column = "ASSIGN_STAFF_FLAG",property = "ASSIGN_STAFF_FLAG",javaType = Integer.class),
            @Result(column = "ASSIGN_POS_FLAG",property = "ASSIGN_POS_FLAG",javaType = Integer.class),
            @Result(column = "ASSIGN_DEPT_FLAG",property = "ASSIGN_DEPT_FLAG",javaType = Integer.class),
            @Result(column = "POSITION_FLAG",property = "POSITION_FLAG",javaType = Integer.class),
            @Result(column = "GROUP_FLAG",property = "GROUP_FLAG",javaType = Integer.class),
            @Result(column = "DEPARTMENT_FLAG",property = "DEPARTMENT_FLAG",javaType = Integer.class),
            @Result(column = "MENUOPERATE_ID",property = "MENUOPERATE_ID",javaType = Long.class),
    })
    List<Map<String,Object>> getNewPermissionList(@Param("cid") Long cid,@Param("opIds") List<Long> opIds,@Param("roleId") Long roleId,@Param("userId") Long userId);

    @Select("SELECT rp.ID,mo.MENUINFO_ID FROM rbac_rolepermission rp LEFT JOIN rbac_menuoperate mo ON rp.MENUOPERATE_ID = mo.ID ${ew.customSqlSegment}")
    @Results({
            @Result(column = "ID",property = "ID",javaType = Long.class),
            @Result(column = "MENUINFO_ID",property = "MENUINFO_ID",javaType = Long.class),
    })
    List<Map<String,Long>> findDefaultOperate(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT COUNT(1) FROM rbac_rolepermission rp LEFT JOIN rbac_menuoperate mo ON rp.MENUOPERATE_ID = mo.ID ${ew.customSqlSegment}")
    Integer findCountByMenuInfoPermission(@Param(Constants.WRAPPER) Wrapper wrapper);

    List<RolePermissionBO> findRolePermissionByUserId(@Param("userId") Long userId);
}
