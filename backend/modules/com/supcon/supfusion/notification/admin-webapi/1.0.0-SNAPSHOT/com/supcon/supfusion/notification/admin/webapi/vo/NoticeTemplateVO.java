package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.notification.admin.service.NoticeProtocolService;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeTemplateVO extends NoticeBaseVO {
    @ApiModelProperty(value = "通知方式", name = "noticeType")
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long noticeType;

    @ApiModelProperty(value = "通知方式返回对象集")
    private NoticeProtocolVO protocol;

    private String protocol_id;
    private String protocol_code;
    private String protocol_name;

    @ApiModelProperty(value = "模板内容", name = "template", example = "这是消息模板${filed}")
    private String template;

    @ApiModelProperty(value = "模板默认参数", name = "params", example = "\"{\"filed\":\"value\"}\"")
    private String params;


}
