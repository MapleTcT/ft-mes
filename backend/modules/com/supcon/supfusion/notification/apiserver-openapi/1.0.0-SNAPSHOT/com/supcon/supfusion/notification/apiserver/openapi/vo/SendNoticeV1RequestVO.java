package com.supcon.supfusion.notification.apiserver.openapi.vo;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@ApiModel(value="消息发送内容")
public class SendNoticeV1RequestVO extends VO {
    /**
     * 消息协议类型
     */
    @NotEmpty(message = "消息发送者不能为空")
    @Length(max = 200, message = "协议类型长度不能超过200")
    @ApiModelProperty(value = "消息发送者", required = true)
    private String sender;
    /**
     * 消息协议类型
     */
    @NotEmpty(message = "消息来源不能为空")
    @Length(max = 200, message = "消息来源长度不能超过200")
    @ApiModelProperty(value = "消息来源", required = true)
    private String source;
    /**
     * 发送方式
     */
    @Length(max = 32, message = "发送方式长度不能超过32")
    @ApiModelProperty(value = "发送方式")
    private String type;
    /**
     * 发送通道
     */
    @Length(max = 32, message = "发送通道长度不能超过32")
    @ApiModelProperty(value = "发送通道")
    private String channel;
    /**
     * 是否使用默认发送通道
     */
    @ApiModelProperty(value = "否使用默认发送通道")
    private Boolean defaultChannel;
    /**
     * 多个发送方式
     */
    @ApiModelProperty(value = "多个发送方式")
    private List<List<String>> multipleType;
    /**
     * 接收人
     */
    @NotNull(message = "接收人不能为空")
    @Size(min = 1, message = "接收人不能为空")
    @ApiModelProperty(value = "接收人")
    private List<String> receivers;
    /**
     * 消息标题
     */
    @ApiModelProperty(value = "消息标题, multipleType生效时生效")
    private String title;
    /**
     * 消息内容
     */
    @ApiModelProperty(value = "消息内容, multipleType生效时生效")
    private String text;
    /**
     * 消息内容
     */
    @ApiModelProperty(value = "消息内容, type生效时生效")
    private JSONObject content;
}
