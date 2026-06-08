package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NoticeProtocolConfigVO extends VO {
    @ApiModelProperty(value = "ID")
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long id;
    @ApiModelProperty(value = "协议ID")
    private String protocol;
    @ApiModelProperty(value = "配置项内容")
    private String configValue;
}
