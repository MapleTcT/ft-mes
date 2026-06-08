package com.supcon.supfusion.organization.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResultVO extends VO {

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
     * 公司信息
     */
    private CompanyResultVO company;
}
