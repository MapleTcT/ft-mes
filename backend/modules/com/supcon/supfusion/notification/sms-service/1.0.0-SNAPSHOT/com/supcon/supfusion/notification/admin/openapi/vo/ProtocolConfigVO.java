package com.supcon.supfusion.notification.admin.openapi.vo;

import lombok.Data;

import java.util.List;

/**
 * ProtocolConfigVO
 *
 * @author OpenAI
 * @date 2021-04-12 18:25:09
 */
@Data
public class ProtocolConfigVO {


    private String protocol;

    private String name;

    private String i18nModule;

    private String i18nKey;

    private String appName;

    private String venderName;

    private String serviceName;

    private String sendUrl;

    private String configUrl;

    private String systemConfigAppCode;

    private String systemConfigCode;

    private String doc;

    private String defaultTemplateCode;

    private List<ProtocolTemplateVO> templates;
}
