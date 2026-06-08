package com.supcon.supfusion.rbac.webapi.vo.userPermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 用户分配权限实体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "用户权限")
public class UserPermissionReVO {
    private static final long serialVersionUID = -1290603299685303831L;

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    /**
     * 用户权限新增集合
     */
    @ApiModelProperty(value = "用户ID")
    List<UserPermissionBaseVO> addList;

    /**
     * 用户权限删除集合
     */
    @ApiModelProperty(value = "用户ID")
    List<UserPermissionBaseVO> deleteList;

}
