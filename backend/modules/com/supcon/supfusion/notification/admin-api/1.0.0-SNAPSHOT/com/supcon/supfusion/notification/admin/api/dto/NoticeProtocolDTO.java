package com.supcon.supfusion.notification.admin.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NoticeProtocolDTO extends DTO {
    /**
     * 协议ID
     */
    private Long id;

    /**
     * 协议类型
     */
    private String protocol;

    /**
     * 协议展示名
     */
    private String name;

    /**
     * appName
     */
    private String appName;

    /**
     * venderName
     */
    private String venderName;

    /**
     * app访问地址
     */
    private String serviceName;

    /**
     * 消息协议发送地址
     */
    private String sendUrl;

    /**
     * 消息协议配置地址
     */
    private String configUrl;

    /**
     * 系统配置唯一标示：systemConfigAppCode + systemConfigCode
     */
    private String systemConfigAppCode;

    /**
     * 系统配置唯一标示：systemConfigAppCode + systemConfigCode
     */
    private String systemConfigCode;

    /**
     * 默认基础模板code
     */
    private String defaultTemplateCode;

    /**
     * 消息内容支持格式, 0 纯文本、1 富文本
     */
    private Integer contentType;

    /**
     * 消息协议说明文档
     */
    private String doc;

    /**
     * 国际化模块名
     */
    private String i18nModule;

    /**
     * 国际化key
     */
    private String i18nKey;

    /**
     * 是否为系统协议
     */
    private Integer system;
}
