package com.supcon.supfusion.organization.service.bo.person;

import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDepartmentBO {

    private DepartmentDetailBO department;

    private DepartmentDetailBO rootDepartment;
}
