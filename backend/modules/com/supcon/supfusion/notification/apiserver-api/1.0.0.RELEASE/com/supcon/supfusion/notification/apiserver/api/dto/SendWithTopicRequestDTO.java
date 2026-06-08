package com.supcon.supfusion.notification.apiserver.api.dto;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class SendWithTopicRequestDTO extends DTO {
    /**
     * 业务方事务编号
     */
    @NotEmpty(message = "消息发送方业务编号不能为空")
    @Length(max = 50, message = "发送方业务编号长度不能超过50")
    @ApiModelProperty(value = "消息发送方业务编号", required = true)
    private String bsmodCode;
    /**
     * 业务模块名称
     */
    @NotEmpty(message = "消息发送方服务名称不能为空")
    @Length(max = 50, message = "发送方服务名称长度不能超过50")
    @ApiModelProperty(value = "消息发送方服务名称", required = true)
    private String bsmodName;
    /**
     * 主题编号
     */
    @NotEmpty(message = "主题编号不能为空")
    @ApiModelProperty(value = "主题编号", required = true)
    private String topicCode;
    /**
     * 发送协议
     */
    @ApiModelProperty(value = "指定当前主题所拥有的发送协议(只能选择当前主题所拥有的协议，超出范围报错)")
    private List<String> protocols;
    /**
     * 消息接收范围
     */
    @ApiModelProperty(value = "如果填写则覆盖当前主题发送范围（可多选）")
    private List<RangeDTO> receivers;
    /**
     * 消息内容入参
     */
    @ApiModelProperty(value = "指定消息模板对应的参数")
    private JSONObject param;
}
