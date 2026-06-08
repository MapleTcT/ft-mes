package com.supcon.supfusion.auth.common.dto;

import lombok.Data;

/**
 * @author caokele
 */
@Data
public class LdapDTO {
    /**
     * 目录类型
     */
    private String directoryType;
    /**
     * 主机名
     */
    private String hostname;
    /**
     * 端口号
     */
    private Integer port;
    /**
     * 是否启用SSL
     */
    private Boolean enableSsl;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 密码
     */
    private String password;
    /**
     * 基本DN
     */
    private String baseDn;
    /**
     * 附加用户DN
     */
    private String attachUserDn;
    /**
     * 附加组DN
     */
    private String attachGroupDn;
    /**
     * LDAP权限
     */
    private String permission;
    /**
     * 默认角色，使用","分隔
     */
    private String defaultRoles;

    private Boolean syncFirst;

    private String adName;

    private String adPassword;
}
