package com.supcon.supfusion.organization.openapi.vo.person;

import io.swagger.annotations.ApiModelProperty;
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
public class PersonDepartmentBaseVO {

    @ApiModelProperty(value = "人员所属部门编码")
    private String code;

    @ApiModelProperty(value = "人员所属部门编码")
    private String name;
}
