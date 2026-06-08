package com.supcon.supfusion.rbac.openapi.vo.roleCustomPermissionRef;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 自定义权限角色关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@ApiModel(description= "自定义权限返回类")
public class RoleCustomPermissionRefVO {

    private static final long serialVersionUID=1L;


    /**
     * 其他限制编码
     */
    @ApiModelProperty(value = "自定义权限限制编码")
    private String customPermissionCode;

    /**
     * 角色权限ID
     */
    @ApiModelProperty(value = "角色权限ID")
    private Long rolepermissionId;


}
