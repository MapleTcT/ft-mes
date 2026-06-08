package com.supcon.supfusion.organization.service.bo.department;

import com.supcon.supfusion.organization.service.bo.company.CompanyBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentBaseInfoBO {

    private String code;

    private String name;

    private String parentCode;

    private SystemCodeBO deptType;

    private Integer valid;

    private String fullPath;

    private CompanyBaseInfoBO company;
}
