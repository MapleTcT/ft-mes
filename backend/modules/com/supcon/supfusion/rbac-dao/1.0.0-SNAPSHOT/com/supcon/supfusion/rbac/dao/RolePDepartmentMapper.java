package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supcon.supfusion.rbac.dao.po.RolePDepartmentPO;
import com.supcon.supfusion.rbac.dao.po.UserPDepartmentPO;
import com.supcon.supfusion.rbac.dao.query.RolePDepartmentQuery;
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
public interface RolePDepartmentMapper extends BaseMapper<RolePDepartmentPO> {

    List<RolePDepartmentPO> findByRolePermissionId(RolePDepartmentQuery rolePDepartmentQuery);
}
