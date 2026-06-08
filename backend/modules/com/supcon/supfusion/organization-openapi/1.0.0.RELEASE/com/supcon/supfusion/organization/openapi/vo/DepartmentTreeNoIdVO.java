package com.supcon.supfusion.organization.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树形结构
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTreeNoIdVO extends VO {

    /**
     * 部门id
     */
    @JsonIgnore
    private Long id;

    /**
     * 部门编码
     */
    @ApiModelProperty(value = "部门编码")
    private String code;

    /**
     * 父部门编码
     */
    @ApiModelProperty(value = "父部门编码")
    private String parentCode;

    /**
     * 部门名称
     */
    @ApiModelProperty(value = "部门名称")
    private String name;

    /**
     * 所属公司code
     */
    @ApiModelProperty(value = "所属公司code")
    private String companyCode;

    /**
     * 部门全路径
     */
    @ApiModelProperty(value = "部门全路径")
    private String fullPath;


    /**
     * 上级部门id（如果上级是公司则为空）
     */
    @JsonIgnore
    private Long parentId;

    /**
     * 下级部门
     */
    @ApiModelProperty(value = "下级部门")
    List<DepartmentTreeNoIdVO> children;

    public List<DepartmentTreeNoIdVO> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }
}
