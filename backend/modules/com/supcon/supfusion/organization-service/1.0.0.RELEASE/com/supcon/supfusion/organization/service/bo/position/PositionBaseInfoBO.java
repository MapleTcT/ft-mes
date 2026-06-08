package com.supcon.supfusion.organization.service.bo.position;

import com.supcon.supfusion.organization.service.bo.company.CompanyBaseInfoBO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PositionBaseInfoBO {

    private String code;

    private String name;

    private String parentCode;

    private Integer valid;

    private String fullPath;

    private CompanyBaseInfoBO company;

    private DepartmentForPositionBaseInfoBO department;
}
