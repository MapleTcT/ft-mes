package com.supcon.supfusion.notification.admin.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
@ApiModel(value="协议基础模板")
public class ProtocolTemplateDTO extends DTO {
    /**
     * 模板名称
     */
    @NotEmpty(message = "模板名称不能为空")
    @Length(max = 32, message = "模板名称长度不能超过32")
    @ApiModelProperty(value = "模板名称", required = true)
    private String name;
    /**
     * 模板编码
     */
    @NotEmpty(message = "模板编码不能为空")
    @Length(max = 32, message = "模板编码长度不能超过32")
    @ApiModelProperty(value = "模板编码", required = true)
    private String code;
    /**
     * 模板内容
     */
    @NotEmpty(message = "模板内容不能为空")
    @ApiModelProperty(value = "模板内容", required = true)
    private String template;
    /**
     * 模板描述
     */
    @Length(max = 200, message = "模板描述长度不能超过200")
    @ApiModelProperty(value = "模板描述", required = false)
    private String description;

}
