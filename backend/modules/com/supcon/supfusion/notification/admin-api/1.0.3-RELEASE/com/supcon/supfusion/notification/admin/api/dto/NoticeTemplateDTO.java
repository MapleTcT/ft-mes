package com.supcon.supfusion.notification.admin.api.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
public class NoticeTemplateDTO {

    @NotEmpty(message = "编码不能为空")
    @Length(max = 50, message = "编码长度不能超过50")
    private String code;

    @NotEmpty(message = "名称不能为空")
    @Length(max = 50, message = "名称长度不能超过50")
    private String name;

    @ApiModelProperty(value = "通知方式", example = "邮件请填写：email；站内信请填写：stationLetter")
    @NotEmpty(message = "通知方式不能为空")
    private String protocol;

    @ApiModelProperty(value = "描述")
    private String memo;

    @NotEmpty(message = "模板内容不能为空")
    @ApiModelProperty(value = "模板内容", example = "邮件请填写如下格式: {\"text\":\"xxx\"}；站内信请填写如下格式: {\"url\":\"http://www.baidu.com\",\"text\":\"xxx\"}")
    private String template;


}
