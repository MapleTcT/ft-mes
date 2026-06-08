package com.supcon.supfusion.auth.webapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;


/**
 * 用户目录连接测试参数模型
 *
 * @author caokele
 */
@Data
@ApiModel("用户目录连接测试参数模型")
public class UserDirectoryConnectVO {

    @NotEmpty(message = "主机名不能为空")
    @Length(max = 200, message = "主机名长度不能超过200")
    @ApiModelProperty(value = "主机名", example = "ldap.supcon.com", required = true)
    private String hostname;

    @NotNull(message = "端口号不能为空")
    @Max(value = 65535, message = "端口号的大小应该在0到65535之间")
    @ApiModelProperty(value = "端口号", example = "389", required = true)
    private Integer port;

    @ApiModelProperty(value = "是否启用SSL", example = "false", required = false)
    private Boolean enableSsl;

    @Length(max = 200, message = "用户名长度不能超过200")
    @ApiModelProperty(value = "用户名", example = "cn=admin,dc=ldap,dc=supcon,dc=com", required = false)
    private String userName;

    @Length(max = 200, message = "密码长度不能超过200")
    @ApiModelProperty(value = "密码", example = "123456", required = false)
    private String password;

    @Length(max = 500, message = "基本DN长度不能超过500")
    @ApiModelProperty(value = "基本DN", example = "dc=ldap,dc=supcon,dc=com", required = false)
    private String baseDn;
}
