package com.supcon.supfusion.notification.app.config.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

import java.util.List;

@Data
public class ProtocolConfigVO extends VO {
    /**
     * 消息协议类型
     */
    private String protocol;
    /**
     * 消息协议展示名称
     */
    private String name;

    /**
     * 协议国际化模块key
     */
    private String i18nModule;

    /**
     * 协议展示名国际化资源key
     */
    private String i18nKey;

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
     * 消息发送接口地址
     */
    private String sendUrl;
    /**
     * 协议配置界面地址
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
     * 消息内容支持格式, 0 纯文本、1 富文本
     */
//    private ProtocolContentType protocolContentType;

    /**
     * 协议说明文档
     */
    private String doc;

    /**
     * 默认基础模板编号
     */

    private String defaultTemplateCode;
    /**
     * 协议基础模板
     */
    private List<ProtocolTemplateVO> templates;
}
