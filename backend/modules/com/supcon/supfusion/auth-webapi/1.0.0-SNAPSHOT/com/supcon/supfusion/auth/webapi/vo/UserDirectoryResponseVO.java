package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户目录响应模型
 *
 * @author caokele
 */
@Data
@ApiModel("用户目录响应模型")
public class UserDirectoryResponseVO {

    @ApiModelProperty(value = "主键id", name = "id", example = "580038889177088")
    private Long id;

    @ApiModelProperty(value = "用户目录名称", example = "LDAP服务器-生态技术部")
    private String directoryName;

    @ApiModelProperty(value = "用户目录类型", example = "ldap")
    private SystemCodeResultDTO directoryType;

    @ApiModelProperty(value = "描述", example = "生态技术部的用户目录")
    private String description;

    @ApiModelProperty(value = "主机名", example = "ldap.supcon.com")
    private String hostname;

    @ApiModelProperty(value = "端口号", example = "389")
    private Integer port;

    @ApiModelProperty(value = "是否启用SSL", example = "false")
    private Boolean enableSsl;

    @ApiModelProperty(value = "用户名", example = "cn=admin,dc=ldap,dc=supcon,dc=com")
    private String userName;

    @ApiModelProperty(value = "密码", example = "123456")
    private String password;

    @ApiModelProperty(value = "基本DN", example = "dc=ldap,dc=supcon,dc=com")
    private String baseDn;

    @ApiModelProperty(value = "附加用户DN", example = "zhangsan")
    private String attachUserDn;

    @ApiModelProperty(value = "附加用户DN", example = "dev")
    private String attachGroupDn;

    @ApiModelProperty(value = "LDAP权限", example = "readOnly")
    private String permission;

    @ApiModelProperty(value = "默认角色，使用半角逗号分隔", example = "zhangsan,lisi")
    private String defaultRoles;

    @ApiModelProperty(value = "排序", example = "1")
    private Double sort;

    @ApiModelProperty(value = "是否启用", example = "true")
    private Boolean enabled;
}
