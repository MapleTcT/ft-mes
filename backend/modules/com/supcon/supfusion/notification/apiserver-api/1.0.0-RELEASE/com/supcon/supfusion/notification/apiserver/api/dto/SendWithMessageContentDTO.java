package com.supcon.supfusion.notification.apiserver.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class SendWithMessageContentDTO extends VO {
    /**
     * 消息协议类型
     */
    @NotEmpty(message = "消息协议类型不能为空")
    @ApiModelProperty(value = "消息协议类型", required = true)
    private String protocol;
    /**
     * 消息协议类型
     */
    @NotEmpty(message = "消息内容不能为空")
    @ApiModelProperty(value = "消息内容不能为空", required = true)
    private String content;
}
