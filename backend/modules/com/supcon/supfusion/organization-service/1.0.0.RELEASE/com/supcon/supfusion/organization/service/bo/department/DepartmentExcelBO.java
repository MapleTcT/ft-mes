package com.supcon.supfusion.organization.service.bo.department;

import com.supcon.supfusion.organization.service.bo.person.OrganizationManagerBO;
import lombok.*;

import java.util.List;

/**
 * 部门Excel导入处理类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentExcelBO {

    /**
     * 部门编码
     */
    private String code;

    /**
     * 部门类型
     */
    private String name;

    /**
     * 父级部门编码
     */
    private String parentCode;

    /**
     * 父级部门名称
     */
    private String parentName;

    /**
     * 部门描述
     */
    private String description;

    /**
     * 负责人
     */
    private List<OrganizationManagerBO> managers;
}
