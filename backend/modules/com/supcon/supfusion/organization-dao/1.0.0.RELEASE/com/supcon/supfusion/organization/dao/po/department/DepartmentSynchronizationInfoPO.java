package com.supcon.supfusion.organization.dao.po.department;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSynchronizationInfoPO {

    private Long id;

    private String code;

    private String parentCode;

    private String name;

    private Integer valid;

    private String modifyTime;

    private String description;

    private String fullPath;

    private Integer layNo;

    private Double sort;

    private String deptType;

    private String companyCode;

    private String companyFullName;

    private String companyShortName;
}