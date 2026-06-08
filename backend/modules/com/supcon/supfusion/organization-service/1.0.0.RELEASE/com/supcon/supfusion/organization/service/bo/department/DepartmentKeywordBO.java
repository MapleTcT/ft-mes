package com.supcon.supfusion.organization.service.bo.department;

import lombok.*;

/**
 * 模糊匹配搜索列表
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentKeywordBO {
    /**
     * 部门id
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 关联的人员数量
     */
    private Long personNum = 0L;
}
