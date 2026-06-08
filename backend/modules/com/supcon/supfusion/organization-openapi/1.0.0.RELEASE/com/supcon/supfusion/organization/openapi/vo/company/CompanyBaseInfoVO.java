package com.supcon.supfusion.organization.openapi.vo.company;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CompanyBaseInfoVO extends VO {

    @ApiModelProperty(value = "公司编码")
    private String code;

    @ApiModelProperty(value = "公司全称")
    private String fullName;

    @ApiModelProperty(value = "公司简称")
    private String shortName;
}
