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
public class PersonCompanyBaseVO {

    @ApiModelProperty(value = "人员所属公司编码")
    private String code;

    @ApiModelProperty(value = "人员所属公司全称")
    private String name;
}
