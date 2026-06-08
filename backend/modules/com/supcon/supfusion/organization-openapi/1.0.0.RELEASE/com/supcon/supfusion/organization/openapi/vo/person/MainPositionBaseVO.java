package com.supcon.supfusion.organization.openapi.vo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MainPositionBaseVO extends VO {

    @ApiModelProperty(value = "主岗编码")
    private String code;

    @ApiModelProperty(value = "主岗名称")
    private String name;
}
