package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 角色指定人员
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
public class RolePStaffDTO implements Serializable {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 人员ID
     */
    private Long staffId;

    /**
     * 角色权限ID
     */
    private Long rolepermissionId;


}
