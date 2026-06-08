package com.supcon.supfusion.rbac.webapi.vo.rolePermission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限前端传送实体
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(description= "角色权限新增类")
public class RolePermissionAddVO {

    private static final long serialVersionUID = 1612073661232529088L;

    /**
     * 角色权限集合
     */
    @ApiModelProperty(value = "角色权限集合")
    List<RolePermissionReVO> list;

}
