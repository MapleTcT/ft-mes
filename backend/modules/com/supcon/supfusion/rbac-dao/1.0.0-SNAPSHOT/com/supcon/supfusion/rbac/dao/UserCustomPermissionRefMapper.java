package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.UserCustomPermissionRefPO;
import com.supcon.supfusion.rbac.dao.po.UserPPositionPO;
import com.supcon.supfusion.rbac.dao.provider.UserCustomPermissionProvider;
import com.supcon.supfusion.rbac.dao.provider.UserPPositionProvider;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 自定义权限用户关联表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public interface UserCustomPermissionRefMapper extends BaseMapper<UserCustomPermissionRefPO> {

    @DeleteProvider(value = UserCustomPermissionProvider.class,method = "deleteCustomPermission")
    void deleteUserCustomPermission(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    @Select("SELECT * FROM rbac_user_custompermission_ref WHERE USERPERMISSION_ID = #{userPermissionId}")
    List<UserCustomPermissionRefPO> findByUserPermissionId(@Param("userPermissionId") Long userPermissionId);
}
