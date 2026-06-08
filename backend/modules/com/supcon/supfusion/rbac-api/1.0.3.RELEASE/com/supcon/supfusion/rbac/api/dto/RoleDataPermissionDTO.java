package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 业务数据权限角色关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class RoleDataPermissionDTO implements Serializable {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 配置内容
     */
    private String configString;

    /**
     * SQL内容
     */
    private String content;

    /**
     * 其他限制编码
     */
    private String dataPermissionCode;

    /**
     * 角色权限ID
     */
    private Long rolepermissionId;


}
