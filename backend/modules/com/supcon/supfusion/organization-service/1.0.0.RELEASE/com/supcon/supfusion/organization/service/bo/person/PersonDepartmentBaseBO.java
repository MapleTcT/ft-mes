package com.supcon.supfusion.organization.service.bo.person;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonDepartmentBaseBO {

    private Long id;

    private String code;

    private String name;
}
