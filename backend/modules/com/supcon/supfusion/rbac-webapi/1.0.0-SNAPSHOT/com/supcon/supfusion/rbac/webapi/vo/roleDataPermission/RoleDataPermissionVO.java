package com.supcon.supfusion.rbac.webapi.vo.roleDataPermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 业务数据权限角色关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "业务数据权限限制返回类")
public class RoleDataPermissionVO {

    private static final long serialVersionUID=1L;


    /**
     * 配置内容
     */
    @ApiModelProperty(value = "配置内容")
    private String configString;

    /**
     * SQL内容
     */
    @ApiModelProperty(value = "SQL内容")
    private String content;

    /**
     * 其他限制编码
     */
    @ApiModelProperty(value = "业务数据权限编码")
    private String dataPermissionCode;

    /**
     * 角色权限ID
     */
    @ApiModelProperty(value = "角色权限ID")
    private Long rolepermissionId;


}
