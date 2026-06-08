package com.supcon.supfusion.organization.webapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.*;

import java.util.List;

/**
 * 岗位详细信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionDetailVO extends VO {

    /**
     * 岗位id
     */
    @ApiModelProperty(value = "岗位id")
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
    @ApiModelProperty(value = "上级岗位id")
    private Long parentId;

    /**
     * 所属公司id
     */
    @ApiModelProperty(value = "公司id")
    private Long companyId;

    /**
     * 关联部门ｉｄ
     */
    @ApiModelProperty(value = "关联部门id")
    private Long depId;

    /**
     * 关联部门名称
     */
    private String depName;

    /**
     * 岗位描述
     */
    @ApiModelProperty(value = "描述")
    private String description;


    /**
     * 组织路径
     */
    @ApiModelProperty(value = "部门全路径")
    private String fullPath;

    /**
     * 统计排序
     */
    @ApiModelProperty(value = "排序")
    private Double sort;

    /**
     * 是否是叶子节点０不是，１是
     */
    @ApiModelProperty(value = "是否叶子节点")
    private Boolean leaf;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;

    @ApiModelProperty(value = "id层级")
    private String layRec;

    @ApiModelProperty(value = "删除标识，是否有效")
    private Boolean valid;
}
