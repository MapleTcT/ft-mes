package com.supcon.supfusion.notification.admin.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@ApiModel(value="协议配置参数")
public class ProtocolConfigDTO extends DTO {
    /**
     * 消息协议类型
     */
    @NotEmpty(message = "协议类型不能为空")
    @Length(max = 13, message = "协议类型长度不能超过13")
    @ApiModelProperty(value = "协议类型", required = true)
    private String protocol;
    /**
     * 消息协议展示名称
     */
    @NotEmpty(message = "协议展示名称不能为空")
    @Length(max = 200, message = "协议展示名称长度不能超过200")
    @ApiModelProperty(value = "协议展示名称", required = true)
    private String name;
    /**
     * 协议国际化模块key
     */
    @Length(max = 100, message = "协议国际化模块key不能超过100")
    @ApiModelProperty(value = "协议国际化模块key", required = false)
    private String i18nModule;

    /**
     * 协议展示名国际化资源key
     */
    @Length(max = 100, message = "协议展示名国际化资源key不能超过100")
    @ApiModelProperty(value = "协议展示名国际化资源key", required = false)
    private String i18nKey;

    /**
     * appName
     */
    @NotEmpty(message = "appName不能为空")
    @Length(max = 128, message = "appName长度不能超过256")
    @ApiModelProperty(value = "appName", required = true)
    private String appName;
    /**
     * venderName
     */
    @NotEmpty(message = "venderName不能为空")
    @Length(max = 256, message = "venderName长度不能超过256")
    @ApiModelProperty(value = "venderName", required = true)
    private String venderName;
    /**
     * app访问地址
     */
    @NotEmpty(message = "serviceName不能为空")
    @Length(max = 256, message = "serviceName长度不能超过256")
    @ApiModelProperty(value = "app访问地址", required = true)
    private String serviceName;
    /**
     * 消息发送接口地址
     */
    @NotEmpty(message = "消息发送接口地址不能为空")
    @Length(max = 256, message = "消息发送接口地址长度不能超过256")
    @ApiModelProperty(value = "消息发送接口地址", required = true)
    private String sendUrl;
    /**
     * 协议配置界面地址
     */
    @NotEmpty(message = "协议配置界面地址不能为空")
    @Length(max = 256, message = "协议配置界面地址长度不能超过256")
    @ApiModelProperty(value = "协议配置界面地址", required = true)
    private String configUrl;
    /**
     * 协议配置界面地址
     */
    @ApiModelProperty(value = "该协议所需要的联系地址项(该字段与发送接口address对应,协议注册时提供该协议所需要的联系地址项,消息发送时通知中心以该字段为最终发送地址.例注册时address:[\"email_address\"],消息发送时address:{\"email_address\":\"xxxxqq.com\"})．如果address字段不填则需要通知APP自行识别supOS人员ID")
    private List<String> addresses;
    /**
     * 默认基础模板编号
     */
    @NotEmpty(message = "默认基础模板编号不能为空")
    @Length(max = 32, message = "默认基础模板编号长度不能超过32")
    @ApiModelProperty(value = "默认基础模板编号", required = true)
    private String defaultTemplateCode;
    /**
     * 协议基础模板
     */
    @NotNull(message = "协议基础模板不能为空")
    @Valid
    @Size(min = 1, message = "协议基础模板不能为空")
    @ApiModelProperty(value = "协议基础模板", required = true)
    private List<ProtocolTemplateDTO> templates;
}
