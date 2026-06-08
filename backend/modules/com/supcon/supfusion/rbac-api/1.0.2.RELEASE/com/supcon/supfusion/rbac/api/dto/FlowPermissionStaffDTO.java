package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

/**
 * <p>
 * 标签表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
@Data
public class FlowPermissionStaffDTO {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 人员ID
     */
    private Long staffId;

    /**
     * 权限ID
     */
    private Long datapermissionId;


}
