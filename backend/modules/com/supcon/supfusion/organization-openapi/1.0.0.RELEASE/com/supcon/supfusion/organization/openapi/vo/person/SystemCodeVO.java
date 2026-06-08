package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SystemCodeVO extends VO {

    @ApiModelProperty(value = "系统编码")
    private String code;

    @ApiModelProperty(value = "系统编码值")
    private String name;
}
