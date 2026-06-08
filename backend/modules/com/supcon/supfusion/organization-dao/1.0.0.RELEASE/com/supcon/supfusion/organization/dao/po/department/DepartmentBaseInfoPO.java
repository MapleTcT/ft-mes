package com.supcon.supfusion.organization.dao.po.department;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentBaseInfoPO {

    private Long id;

    private String code;

    private String parentCode;

    private String name;

    private Integer valid;

    private String description;

    private String fullPath;

    private String deptType;

    private String companyCode;

    private String companyFullName;

    private String companyShortName;
}
