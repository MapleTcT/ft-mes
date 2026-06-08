package com.supcon.supfusion.organization.service.bo.department;

import lombok.*;

/**
 * 部门修改位置PO
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentLocationBO {
    /**
     * 部门id
     */
    private Long id;

    /**
     * 前续部门id
     */
    private Long upId;

    /**
     * 父级部门id
     */
    private Long parentId;
}
