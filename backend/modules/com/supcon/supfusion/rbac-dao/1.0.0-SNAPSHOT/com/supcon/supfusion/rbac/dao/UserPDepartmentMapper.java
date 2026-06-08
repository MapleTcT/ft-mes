package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.UserDataPermissionPO;
import com.supcon.supfusion.rbac.dao.po.UserPDepartmentPO;
import com.supcon.supfusion.rbac.dao.provider.UserPDepartmentProvider;
import com.supcon.supfusion.rbac.dao.provider.UserPPositionProvider;
import com.supcon.supfusion.rbac.dao.provider.UserPermissionProvider;
import com.supcon.supfusion.rbac.dao.query.UserPDepartmentQuery;
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
public interface UserPDepartmentMapper extends BaseMapper<UserPDepartmentPO> {

    List<UserPDepartmentPO> findByUserPermissionId(UserPDepartmentQuery userPDepartmentQuery);

    @DeleteProvider(value = UserPDepartmentProvider.class,method = "deleteUserPDepartment")
    void deleteUserPDepartment(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);
}
