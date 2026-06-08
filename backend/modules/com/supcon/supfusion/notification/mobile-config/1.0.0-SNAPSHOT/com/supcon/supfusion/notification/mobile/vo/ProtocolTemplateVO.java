package com.supcon.supfusion.notification.mobile.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class ProtocolTemplateVO extends VO {
    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板名称国际化key
     */
    private String i18nKey;

    /**
     * 模板编码
     */
    private String code;
    /**
     * 模板内容
     */

    private String template;
    /**
     * 模板描述
     */
    private String description;

}
