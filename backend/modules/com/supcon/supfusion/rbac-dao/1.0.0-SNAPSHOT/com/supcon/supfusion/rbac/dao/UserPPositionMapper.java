package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.RolePPositionPO;
import com.supcon.supfusion.rbac.dao.po.UserPPositionPO;
import com.supcon.supfusion.rbac.dao.provider.UserPPositionProvider;
import com.supcon.supfusion.rbac.dao.query.UserPPositionQuery;
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
public interface UserPPositionMapper extends BaseMapper<UserPPositionPO> {

    @DeleteProvider(value = UserPPositionProvider.class,method = "deleteUserPPosition")
    void deleteUserPPosition(@Param("cid") Long cid, @Param("opIds") List<Long> opIds, @Param("roleId") Long roleId, @Param("userId") Long userId);

    List<UserPPositionPO> findByUserPermissionId(UserPPositionQuery userPPositionQuery);
}
