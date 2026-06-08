package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 角色指定岗位表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
public class RolePPositionDTO implements Serializable {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 包含下级
     */
    private Boolean includeLower;

    /**
     * 岗位ID
     */
    private Long positionId;

    /**
     * 角色权限ID
     */
    private Long rolepermissionId;


}
