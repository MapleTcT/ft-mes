package com.supcon.supfusion.organization.service.bo.person;


import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RelationDepartmentBO {
    /**
     * 部门全路径
     */
    @ApiModelProperty(value = "部门全路径")
    private String fullPath;

    /**
     * 是否是当前岗位的部门
     */
    private Boolean currentPosition = false;
    @ApiModelProperty(value = "部门id")
    private Long id;

    @ApiModelProperty(value = "公司id")
    private Long companyId;
    @ApiModelProperty(value = "部门编码")
    private String code;
    @ApiModelProperty(value = "部门名称")
    private String name;
    @ApiModelProperty(value = "上级部门id")
    private Long parentId;
    @ApiModelProperty(value = "id层级")
    private String layRec;

}
