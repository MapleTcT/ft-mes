package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 角色用户表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface RoleUserMapper extends BaseMapper<RoleUserPO> {

    @Select("SELECT roleUser.id,roleUser.FROM_POSITION,roleUser.role_id,roleUser.user_id,person_name,person_code,user_name,roleUser.modify_time FROM rbac_roleuser roleUser LEFT JOIN  rbac_role role ON roleUser.role_id = role.id ${ew.customSqlSegment}")
    @Results({
            @Result(property = "role", column = "role_id",one = @One(select = "com.supcon.supfusion.rbac.dao.RoleMapper.findParent")),
            @Result(property = "roleId", column = "role_id",javaType = Long.class)
    })
    IPage<RoleUserPO> getRoleUsersPage(Page<RoleUserPO> page, @Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT ru.PERSON_NAME PERSONNAME,ru.PERSON_CODE PERSONCODE,ru.USER_NAME USERNAME FROM rbac_roleuser ru LEFT JOIN rbac_role role ON ru.ROLE_ID = role.ID ${ew.customSqlSegment}")
    List<Map<String,Object>> exportData(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT ru.PERSON_NAME PERSONNAME,ru.PERSON_CODE PERSONCODE,ru.USER_NAME USERNAME FROM rbac_roleuser ru LEFT JOIN rbac_role role ON ru.ROLE_ID = role.ID ${ew.customSqlSegment}")
    List<Map<String,Object>> exportDataPage(Page<RoleUserPO> page,@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT * FROM rbac_roleuser ru LEFT JOIN rbac_role role ON ru.ROLE_ID = role.ID ${ew.customSqlSegment}")
    List<RoleUserPO> findUserByRoleCode(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT * FROM rbac_roleuser")
    List<RoleUserPO> getAllRoleUser();
}
