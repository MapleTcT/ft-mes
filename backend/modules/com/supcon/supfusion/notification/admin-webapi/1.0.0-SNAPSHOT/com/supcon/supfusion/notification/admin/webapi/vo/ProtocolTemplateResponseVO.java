package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ApiModel
@ToString
public class ProtocolTemplateResponseVO extends VO {
    @ApiModelProperty("模板ID")
    private String id;

    @ApiModelProperty("模板标题")
    private String name;

    @ApiModelProperty("模板内容")
    private String template;
}
