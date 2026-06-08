package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.UserCustomPermissionRefPO;
import com.supcon.supfusion.rbac.dao.po.UserDataPermissionPO;
import com.supcon.supfusion.rbac.dao.provider.UserCustomPermissionProvider;
import com.supcon.supfusion.rbac.dao.provider.UserDataPermissionProvider;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 业务数据权限用户关联表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public interface UserDataPermissionMapper extends BaseMapper<UserDataPermissionPO> {

    @DeleteProvider(value = UserDataPermissionProvider.class,method = "deleteUserDataPermission")
    void deleteUserDataPermission(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @Select("SELECT * FROM rbac_user_datapermission WHERE USERPERMISSION_ID = #{userPermissionId}")
    List<UserDataPermissionPO> findByUserPermissionId(@Param("userPermissionId") Long userPermissionId);
}
