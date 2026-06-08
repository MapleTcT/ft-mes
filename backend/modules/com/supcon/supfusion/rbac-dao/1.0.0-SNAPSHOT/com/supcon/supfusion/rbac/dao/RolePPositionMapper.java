package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.RolePPositionPO;
import com.supcon.supfusion.rbac.dao.po.UserPPositionPO;
import com.supcon.supfusion.rbac.dao.query.RolePPositionQuery;
import com.supcon.supfusion.rbac.dao.query.UserPPositionQuery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 角色指定岗位表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface RolePPositionMapper extends BaseMapper<RolePPositionPO> {

    List<RolePPositionPO> findByRolePermissionId(RolePPositionQuery rolePPositionQuery);
}
