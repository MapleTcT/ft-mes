package com.supcon.supfusion.notification.apiserver.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collection;
import java.util.List;

@Data
public class SendWithMessageRequestDTO extends DTO {
    /**
     * 业务方事务编号
     */
    @NotEmpty(message = "消息发送方业务编号不能为空")
    @ApiModelProperty(value = "消息发送方业务编号", required = true)
    private String bsmodCode;
    /**
     * 业务模块名称
     */
    @NotEmpty(message = "消息发送方服务名称不能为空")
    @ApiModelProperty(value = "消息发送方服务名称", required = true)
    private String bsmodName;
    /**
     * 消息接收范围
     */
    @NotNull(message = "消息接收范围不能为空")
    @Size(min = 1, message = "消息接收范围不能为空")
    @ApiModelProperty(value = "消息接收范围")
    private Collection<RangeDTO> receivers;
    /**
     * 消息内容入参
     */
    @NotNull(message = "消息内容列表不能为空")
    @Size(min = 1, message = "消息内容列表不能为空")
    @ApiModelProperty(value = "消息内容列表")
    @Valid
    private Collection<SendWithMessageContentDTO> contents;
}
