package com.supcon.supfusion.organization.dao.po.position;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionSynchronizationInfoPO {

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

    private String deptCode;

    private String deptName;

    private String companyCode;

    private String companyFullName;

    private String companyShortName;
}