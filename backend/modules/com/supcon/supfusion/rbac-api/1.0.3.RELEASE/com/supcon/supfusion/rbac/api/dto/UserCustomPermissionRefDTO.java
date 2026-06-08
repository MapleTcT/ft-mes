package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 自定义权限用户关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class UserCustomPermissionRefDTO implements Serializable {


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
     * 用户权限ID
     */
    private Long userpermissionId;


}
