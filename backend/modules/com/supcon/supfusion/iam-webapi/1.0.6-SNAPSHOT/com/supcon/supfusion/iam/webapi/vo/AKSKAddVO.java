package com.supcon.supfusion.iam.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
public class AKSKAddVO extends VO {
    @ApiModelProperty(value = "APP_ID")
    @NotEmpty(message = "APP_ID不能为空")
    @Length(max = 64)
    private String appId;
    @ApiModelProperty(value = "描述")
    private String description;
}