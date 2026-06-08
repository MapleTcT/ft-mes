package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import org.apache.ibatis.annotations.*;

import javax.management.relation.Role;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface RoleMapper extends BaseMapper<RolePO> {

    @Select("SELECT id,name,code,role_Type,version,description FROM rbac_role ${ew.customSqlSegment}")
    @Results({
            @Result(property = "tags", column = "id",many = @Many(select = "com.supcon.supfusion.rbac.dao.TagMapper.findTagName")),
            @Result(property = "id", column = "id")
    })
    IPage<RolePO> getRolePage(Page<RolePO> page, @Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT id,name,code FROM rbac_role WHERE id = #{id} and valid = 1")
    RolePO findParent(@Param("id") Long id);

    @Select("SELECT role.* FROM rbac_roleuser ru LEFT JOIN rbac_role role ON ru.ROLE_ID = role.ID ${ew.customSqlSegment}")
    List<RolePO> findRoleUserByUserId(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT r.id,r.name FROM rbac_role r LEFT JOIN rbac_roleuser ru ON ru.ROLE_ID =r.ID LEFT JOIN rbac_rolepermission rr ON rr.ROLE_ID = ru.ROLE_ID ${ew.customSqlSegment}")
    List<RolePO> getRoleByUserPermission(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT r.id,r.name FROM rbac_role r LEFT JOIN rbac_flow_permission fp ON fp.TYPE_ID =r.ID ${ew.customSqlSegment}")
    List<RolePO> getRoleByUserPermissionFlow(@Param(Constants.WRAPPER) Wrapper wrapper);
}
