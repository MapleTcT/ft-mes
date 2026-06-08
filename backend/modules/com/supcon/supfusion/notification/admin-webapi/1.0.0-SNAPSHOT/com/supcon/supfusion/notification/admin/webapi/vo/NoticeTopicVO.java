package com.supcon.supfusion.notification.admin.webapi.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeTopicVO extends NoticeBaseVO {
    @ApiModelProperty(value = "通知方式返回对象集")
    private NoticeProtocolVO protocol;

    private String protocolId;
    private String protocolCode;
    private String protocolName;
    private String receiver;

    @ApiModelProperty(value = "消息模板返回对象集")
    private NoticeTemplateVO template;

    private String templateId;
    private String templateCode;
    private String templateName;
    private Map<String, List> receviers;

//    @ApiModelProperty(value = "主题对应通知方式的模板",example="[1000,1111,1222]")
//    private Map<Long,Long> topicTmplMap;


}
