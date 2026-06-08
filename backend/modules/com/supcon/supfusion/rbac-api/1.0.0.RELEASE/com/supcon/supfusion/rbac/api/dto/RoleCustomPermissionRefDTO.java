package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 自定义权限角色关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class RoleCustomPermissionRefDTO implements Serializable {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 其他限制编码
     */
    private String customPermissionCode;

    /**
     * 角色权限ID
     */
    private Long rolepermissionId;


}
