package com.supcon.supfusion.organization.service.bo.person;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MainPositionBO {
    /**
     * 岗位全路径
     */
    @ApiModelProperty(value = "岗位全路径")
    private String fullPath;

    /**
     * 是否是主岗位
     */
    @ApiModelProperty(value = "是否是主岗")
    private Boolean mainPosition = false;
    @ApiModelProperty(value = "岗位id")
    private Long id;
    @ApiModelProperty(value = "公司id")
    private Long companyId;
    @ApiModelProperty(value = "岗位编码")
    private String code;
    @ApiModelProperty(value = "岗位名称")
    private String name;
    @ApiModelProperty(value = "上级岗位id")
    private Long parentId;
    @ApiModelProperty(value = "关联的部门id")
    private Long depId;
    @ApiModelProperty(value = "id层级")
    private String layRec;

}
