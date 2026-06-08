package com.supcon.supfusion.organization.service.bo.department;


import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
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
public class DepartmentDetailBO {
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
    private String type;

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
    List<PositionAddPO> relPos;

    /**
     * 是否是叶子节点０不是，１是
     */
    private Boolean leaf;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;

    /**
     * 上级部门编码（excel中使用）
     */
    private String parentCode;
}
