package com.supcon.supfusion.organization.service.bo.position;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionSynchronizationInfoBO {

    private String code;

    private String parentCode;

    private String name;

    private Integer valid;

    private String modifyTime;

    private String description;

    private String fullPath;

    private Integer layNo;

    private Double sort;

    private DepartmentForPositionSynchronizationInfoBO department;

    private CompanyForPositionSynchronizationInfoBO company;
}
