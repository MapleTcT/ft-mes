package com.supcon.supfusion.organization.service.bo.person;


import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailBO;
import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonManagerBO {

    private Long id;

    private String name;

    private String code;

    List<DepartmentDetailBO> departments;

    List<PositionDetailBO> positions;
}
