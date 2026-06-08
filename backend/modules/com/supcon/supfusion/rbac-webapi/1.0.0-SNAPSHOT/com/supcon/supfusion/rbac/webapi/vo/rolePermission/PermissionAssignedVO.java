package com.supcon.supfusion.rbac.webapi.vo.rolePermission;

import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description= "已分配角色权限返回类")
public class PermissionAssignedVO {

    @ApiModelProperty(value = "顶级菜单名")
    private String name;

    @ApiModelProperty(value = "权限信息")
    List<MenuInfoAssignVO> ops;
}
