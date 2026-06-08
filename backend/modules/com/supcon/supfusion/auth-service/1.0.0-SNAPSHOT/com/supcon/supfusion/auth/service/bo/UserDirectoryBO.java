package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

/**
 * 用户目录
 *
 * @author caokele
 */
@Data
public class UserDirectoryBO {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 目录名称
     */
    private String directoryName;
    /**
     * 目录类型
     */
    private String directoryType;
    /**
     * 排序
     */
    private Double sort;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 是否有效
     */
    private Boolean valid;
    /**
     * 描述
     */
    private String description;
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

    /**
     * 是否需要同步
     * 1 需要同步
     * 0 不需要同步
     */
    private Boolean syncFirst;
}
