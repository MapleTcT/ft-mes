package com.supcon.supfusion.organization.service.bo.position;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionDetailInfoBO {

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

    private List<PositionRoleBaseBO> roles;
}