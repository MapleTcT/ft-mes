package com.supcon.supfusion.rbac.webapi.vo.MenuInfo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description= "菜单排序类")
public class MenuSortVO extends VO {
    private static final long serialVersionUID = -2251152419345277483L;

    @ApiModelProperty(value = "父菜单ID")
    private String parentId;

    @ApiModelProperty(value = "上一个菜单的排序号")
    private String prevId;

    @ApiModelProperty(value = "下一个菜单的排序号")
    private String nextId;

    @ApiModelProperty(value = "当前菜单的排序号")
    private String currentId;

    @ApiModelProperty(value = "supfusion菜单配置")
    private Boolean supfusion;
}
