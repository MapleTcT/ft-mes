package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.RolePPositionPO;
import com.supcon.supfusion.rbac.dao.po.RolePStaffPO;
import com.supcon.supfusion.rbac.dao.query.RolePStaffQuery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 角色指定人员 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public interface RolePStaffMapper extends BaseMapper<RolePStaffPO> {

    List<RolePStaffPO> findByRolePermissionId(RolePStaffQuery rolePStaffQuery);
}
