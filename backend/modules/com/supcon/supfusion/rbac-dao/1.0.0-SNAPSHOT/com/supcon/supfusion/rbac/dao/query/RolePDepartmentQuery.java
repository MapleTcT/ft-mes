package com.supcon.supfusion.rbac.dao.query;

import lombok.Data;

import java.util.List;

/**
 * <p>
 * 工作流数据权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class RolePDepartmentQuery {


    private Long id;

    private Integer version;

    private Boolean includeLower;

    private Long departmentId;

    private Long rolePermissionId;

    private List<Long> rolePermissionIds;
}
