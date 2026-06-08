package com.supcon.supfusion.organization.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 岗位树形结构
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionTreeNoIdVO extends VO {
    /**
     * 岗位id
     */
    @JsonIgnore
    private Long id;

    /**
     * 岗位编码
     */
    @ApiModelProperty(value = "岗位编码")
    private String code;

    /**
     * 岗位名称
     */
    @ApiModelProperty(value = "岗位名称")
    private String name;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    @JsonIgnore
    private Long parentId;

    /**
     * 上级岗位code（如果上级是公司则为空）
     */
    @ApiModelProperty(value = "上级岗位code")
    private String parentCode;

    /**
     * 所属公司id
     */
    @JsonIgnore
    private Long companyId;

    /**
     * 所属公司code
     */
    @ApiModelProperty(value = "所属公司code")
    private String companyCode;

    /**
     * 关联部门code
     */
    @ApiModelProperty(value = "关联部门code")
    private String depCode;

    /**
     * 描述
     */
    @ApiModelProperty(value = "描述")
    private String description;

    /**
     * 岗位名称全路径
     */
    @ApiModelProperty(value = "岗位名称全路径")
    private String fullPath;

    /**
     * 下级岗位
     */
    @ApiModelProperty(value = "下级岗位")
    List<PositionTreeNoIdVO> children;

    public List<PositionTreeNoIdVO> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }
}
