package com.supcon.supfusion.organization.webapi.vo.department;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import com.supcon.supfusion.organization.webapi.vo.position.PositionDetailVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

/**
 * 部门详细信息
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDetailVO extends VO {

    /**
     * 部门id
     */
    @ApiModelProperty(value = "部门id")
    private Long id;

    /**
     * 部门编码
     */
    @ApiModelProperty(value = "部门编码")
    private String code;

    /**
     * 部门名称
     */
    @ApiModelProperty(value = "部门名称")
    private String name;

    /**
     * 部门类型码
     */
    @ApiModelProperty(value = "部门类型")
    private String type;

    /**
     * 部门类型名
     */
    private String typeName;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    @ApiModelProperty(value = "上级部门id")
    private Long parentId;

    /**
     * 所属公司id
     */
    @ApiModelProperty(value = "公司id")
    private Long companyId;

    /**
     * 部门描述
     */
    @ApiModelProperty(value = "部门描述")
    private String description;

    /**
     * 负责人id
     */
    private Long managerId;

    /**
     * 负责人名称
     */
    private String managerName;

    /**
     * 组织路径
     */
    @ApiModelProperty(value = "部门全路径")
    private String fullPath;

    /**
     * 统计排序
     */
    @ApiModelProperty(value = "顺序号")
    private Double sort;

    /**
     * 关联的岗位
     */
    List<PositionDetailVO> relPos;

    /**
     * 是否是叶子节点０不是，１是
     */
    @ApiModelProperty(value = "是否叶子")
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
