package com.supcon.supfusion.notification.mobile.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class ProtocolTemplateDto extends DTO {
    /**
     * 模板名称
     */
    @NotEmpty(message = "模板名称不能为空")
    @Length(max = 50, message = "模板名称长度不能超过50")
    private String name;

    /**
     * 模板名称国际化key
     */
    @Length(max = 100, message = "模板名称国际化key长度不能超过100")
    private String i18nKey;

    /**
     * 模板编码
     */
    @NotEmpty(message = "模板编码不能为空")
    @Length(max = 50, message = "模板编码长度不能超过50")
    private String code;
    /**
     * 模板内容
     */
    @NotEmpty(message = "模板内容不能为空")
    private String template;
    /**
     * 模板描述
     */
    @Length(max = 200, message = "模板描述长度不能超过200")
    private String description;

}
