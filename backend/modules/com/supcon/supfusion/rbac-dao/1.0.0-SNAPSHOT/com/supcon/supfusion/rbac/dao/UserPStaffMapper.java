package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.RolePPositionPO;
import com.supcon.supfusion.rbac.dao.po.UserPStaffPO;
import com.supcon.supfusion.rbac.dao.provider.UserPPositionProvider;
import com.supcon.supfusion.rbac.dao.provider.UserPStaffProvider;
import com.supcon.supfusion.rbac.dao.query.UserPStaffQuery;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface UserPStaffMapper extends BaseMapper<UserPStaffPO> {

    @DeleteProvider(value = UserPStaffProvider.class,method = "deleteUserPStaff")
    void deleteUserPStaff(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    List<UserPStaffPO> findByUserPermissionId(UserPStaffQuery userPStaffQuery);
}
