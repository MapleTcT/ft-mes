package com.supcon.supfusion.organization.service.bo.department;

import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.position.CompanyForPositionSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.position.DepartmentForPositionBaseInfoBO;
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
public class DepartmentDetailInfoBO {

    private String code;

    private String parentCode;

    private String name;

    private Integer valid;

    private String modifyTime;

    private String description;

    private String fullPath;

    private Integer layNo;

    private Double sort;

    private SystemCodeBO deptType;

    private CompanyForPositionSynchronizationInfoBO company;

    private List<ManagerForDepartmentSynchronizationInfoBO> managers;

    private List<DepartmentForPositionBaseInfoBO> positons;
}
