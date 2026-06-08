package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
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
    private Long id;

    /**
     * 部门编码
     */
    private String code;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门类型码
     */
    private Integer type;

    /**
     * 部门类型名
     */
    private String typeName;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    private Long parentId;

    /**
     * 所属公司id
     */
    private Long companyId;

    /**
     * 部门描述
     */
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
    private String fullPath;

    /**
     * 统计排序
     */
    private Double sort;

    /**
     * 关联的岗位
     */
    List<PositionDetailVO> relPos;
}
