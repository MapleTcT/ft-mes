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
public class ProtocolTemplateCountResponseVO extends VO {
    @ApiModelProperty("模板个数")
    private Integer count;
}
