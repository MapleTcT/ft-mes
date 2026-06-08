package com.supcon.supfusion.notification.admin.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.admin.common.bean.ProtocolContentType;
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
@ApiModel(value = "协议配置参数")
public class ProtocolConfigVO extends VO {
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
    @Length(max = 50, message = "协议展示名称长度不能超过50")
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
    @Length(max = 128, message = "appName长度不能超过128")
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
    @Length(max = 256, message = "协议配置界面地址长度不能超过256")
    @ApiModelProperty(value = "协议配置界面地址", required = false)
    private String configUrl;
    /**
     * 系统配置唯一标示：systemConfigAppCode + systemConfigCode
     */
    @Length(max = 256, message = "systemConfigAppCode长度不能超过256")
    @ApiModelProperty(value = "systemConfigAppCode", required = false)
    private String systemConfigAppCode;
    /**
     * 系统配置唯一标示：systemConfigAppCode + systemConfigCode
     */
    @Length(max = 256, message = "systemConfigCode长度不能超过256")
    @ApiModelProperty(value = "systemConfigCode", required = false)
    private String systemConfigCode;

    /**
     * 消息内容支持格式, 0 纯文本、1 富文本
     */
    @ApiModelProperty(value = "消息内容支持格式", required = false)
    private ProtocolContentType protocolContentType;

    /**
     * 协议说明文档
     */
    @ApiModelProperty(value = "协议说明文档", required = false)
    private String doc;

    /**
     * 默认基础模板编号
     */
    @NotEmpty(message = "默认基础模板编号不能为空")
    @Length(max = 50, message = "默认基础模板编号长度不能超过50")
    @ApiModelProperty(value = "默认基础模板编号", required = true)
    private String defaultTemplateCode;
    /**
     * 协议基础模板
     */
    @NotNull(message = "协议基础模板不能为空")
    @Valid
    @Size(min = 1, message = "协议基础模板不能为空")
    @ApiModelProperty(value = "协议基础模板", required = true)
    private List<ProtocolTemplateVO> templates;
}
