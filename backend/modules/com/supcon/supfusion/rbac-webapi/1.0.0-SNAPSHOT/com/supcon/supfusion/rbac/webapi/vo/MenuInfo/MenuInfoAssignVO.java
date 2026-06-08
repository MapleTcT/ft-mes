package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.supcon.supfusion.rbac.webapi.vo.menuOperate.MenuOperateAssignVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description= "分配的权限信息")
public class MenuInfoAssignVO {

    @ApiModelProperty(value = "菜单名")
    private String name;

    @ApiModelProperty(value = "菜单ID")
    private Long menuInfoId;

    @ApiModelProperty(value = "操作")
    private MenuOperateAssignVO op;

    @ApiModelProperty(value = "国际化名")
    private String nameDisplay;
}
