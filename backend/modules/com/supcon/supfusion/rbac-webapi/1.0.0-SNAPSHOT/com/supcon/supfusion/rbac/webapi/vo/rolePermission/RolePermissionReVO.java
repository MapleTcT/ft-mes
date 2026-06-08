package com.supcon.supfusion.rbac.webapi.vo.rolePermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限实体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "角色权限")
public class RolePermissionReVO {
    private static final long serialVersionUID = -1290603299685303831L;

    /**
     * 角色ID
     */
    @ApiModelProperty(value = "角色ID")
    private Long roleId;

    /**
     * 角色权限新增集合
     */
    @ApiModelProperty(value = "角色权限新增集合")
    List<RolePermissionBaseVO> addList;

    /**
     * 角色权限删除集合
     */
    @ApiModelProperty(value = "角色权限删除集合")
    List<RolePermissionBaseVO> deleteList;

}
