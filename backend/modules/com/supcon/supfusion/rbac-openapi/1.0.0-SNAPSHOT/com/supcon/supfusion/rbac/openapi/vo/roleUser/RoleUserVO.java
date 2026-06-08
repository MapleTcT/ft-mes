package com.supcon.supfusion.rbac.openapi.vo.roleUser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.rbac.openapi.vo.role.RoleUserRoleVO;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleUserVO {

    //private Long id;

    //private RoleUserRoleVO role;

    private UserDetailVO user;

    //private Integer fromPosition;
}
