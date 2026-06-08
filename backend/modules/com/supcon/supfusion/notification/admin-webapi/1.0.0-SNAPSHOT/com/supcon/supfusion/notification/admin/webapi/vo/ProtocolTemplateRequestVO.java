package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@ApiModel
@ToString
public class ProtocolTemplateRequestVO extends VO {
    @ApiModelProperty("模板标题")
    @NotEmpty(message = "模板标题不能为空")
    private String name;

    @ApiModelProperty("模板内容")
    @NotEmpty(message = "模板内容不能为空")
    private String template;

    @ApiModelProperty("协议ID")
    @NotEmpty(message = "协议ID不能为空")
    @Pattern(regexp = "\\d+", message = "id只能为数字")
    private String protocolId;
}
