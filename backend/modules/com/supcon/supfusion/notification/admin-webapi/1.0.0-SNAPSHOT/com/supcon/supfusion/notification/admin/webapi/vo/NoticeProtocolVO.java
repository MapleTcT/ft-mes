package com.supcon.supfusion.notification.admin.webapi.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeProtocolVO {
    @ApiModelProperty(value = "协议编码")
    private String protocol;
    @ApiModelProperty(value = "协议名称")
    private String name;
    @ApiModelProperty(value = "app名称")
    private String appName;
    @ApiModelProperty(value = "开发商")
    private String venderName;
    @ApiModelProperty(value = "发送接口")
    private String sendUrl;
    @ApiModelProperty(value = "配置接口")
    private String configUrl;


}
