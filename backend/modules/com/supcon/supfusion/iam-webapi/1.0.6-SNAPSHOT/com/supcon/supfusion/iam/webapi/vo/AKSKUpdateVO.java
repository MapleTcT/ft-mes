package com.supcon.supfusion.iam.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
public class AKSKUpdateVO extends VO {
    @ApiModelProperty(value = "描述")
    private String description;
}