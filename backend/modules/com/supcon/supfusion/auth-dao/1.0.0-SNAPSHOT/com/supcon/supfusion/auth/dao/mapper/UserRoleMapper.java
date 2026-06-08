package com.supcon.supfusion.auth.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.auth.dao.po.UserPO;
import com.supcon.supfusion.auth.dao.po.UserRolePO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserRoleMapper extends BaseMapper<UserRolePO> {

    @Select("SELECT auth_user.* FROM auth_user_role,auth_user WHERE auth_user_role.user_id= auth_user.id and auth_user_role.role_id = #{roleId}")
    List<UserPO> selectUserRole(@Param("roleId") Long roleId);


}
