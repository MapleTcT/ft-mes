package com.supcon.supfusion.rbac.webapi.vo.userPermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限前端传送实体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "用户权限新增类")
public class UserPermissionAddVO {

    private static final long serialVersionUID = 1612073661232529088L;

    /**
     * 用户权限集合
     */
    @ApiModelProperty(value = "用户权限集合")
    List<UserPermissionReVO> list;

}
