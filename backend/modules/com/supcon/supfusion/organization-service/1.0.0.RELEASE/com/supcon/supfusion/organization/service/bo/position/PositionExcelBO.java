package com.supcon.supfusion.organization.service.bo.position;

import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import lombok.*;

import java.util.List;

/**
 * 岗位Excel导入处理类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionExcelBO {

    /**
     * 编码
     */
    private String code;

    /**
     * 类型
     */
    private String name;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 父级编码
     */
    private String parentCode;

    /**
     * 父级名称
     */
    private String parentName;

    /**
     * 描述
     */
    private String description;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;

    /**
     * 角色
     */
    private List<String> roleCodes;

    /**
     * 角色 名称
     */
    private List<String> roleNames;
}
