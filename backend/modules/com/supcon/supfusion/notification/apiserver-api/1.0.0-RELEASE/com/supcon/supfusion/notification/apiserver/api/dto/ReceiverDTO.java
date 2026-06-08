package com.supcon.supfusion.notification.apiserver.api.dto;

import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@ApiModel(value = "消息接收人")
public class ReceiverDTO {
    /**
     * 消息唯一ID
     */
    @NotEmpty(message = "消息唯一ID不能为空")
    @ApiModelProperty(value = "消息唯一ID", required = true)
    private String messageId;
    /**
     * 消息发送状态
     */
    @ApiModelProperty(value = "sendStatus")
    private SendStatus sendStatus;
    /**
     * 消息阅读状态
     */
    @ApiModelProperty(value = "消息阅读状态")
    private ReadStatus readStatus;
    /**
     * 失败原因,sendStatus为FAIL时生效
     */
    @ApiModelProperty(value = "失败原因,sendStatus为FAIL时生效")
    private String errorMessage;
}
