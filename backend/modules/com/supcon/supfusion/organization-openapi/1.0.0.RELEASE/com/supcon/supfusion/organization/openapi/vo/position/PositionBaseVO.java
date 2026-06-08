package com.supcon.supfusion.organization.openapi.vo.position;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
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
public class PositionBaseVO extends VO {
    @ApiModelProperty(value = "岗位编号")
    private String code;

    @ApiModelProperty(value = "岗位名称")
    private String name;
}
