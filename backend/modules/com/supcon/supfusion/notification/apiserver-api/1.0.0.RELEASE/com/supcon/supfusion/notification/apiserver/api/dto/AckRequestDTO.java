package com.supcon.supfusion.notification.apiserver.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@ApiModel(value = "消息状态")
public class AckRequestDTO extends DTO {
    /**
     * 消息协议类型
     */
    @NotEmpty(message = "消息协议类型不能为空")
    @ApiModelProperty(value = "消息协议类型", required = true)
    private String protocol;
    /**
     * 消息发送时间
     */
    @NotEmpty(message = "消息发送时间不能为空")
    @ApiModelProperty(value = "消息发送时间", required = true)
    private String time;
    /**
     * 消息接收人
     */
    @Valid
    @NotNull(message = "消息接收人不能为空")
    @Size(min = 1, message = "消息接收人不能为空")
    @ApiModelProperty(value = "消息接收人", required = true)
    private List<ReceiverDTO> receivers;
}
