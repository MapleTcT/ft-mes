package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 业务数据权限角色权限配置临时表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
public class DataPermissionRshowDTO implements Serializable {


    /**
     * 主键ID
     */
    private Long id;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 删除者
     */
    private String deleteStaffId;

    /**
     * 修改者
     */
    private String modifyStaffId;

    /**
     * 创建者
     */
    private String createStaffId;


    /**
     * 是否启用
     */
    private Boolean isAssigned;

    private String layRec;

    /**
     * 是否包含上下级
     */
    private String isIncludeSub;

    /**
     * 操作ID
     */
    private Long operateId;

    /**
     * 编码值
     */
    private String valueCode;

    /**
     * 标题值
     */
    private String valueTitle;

    /**
     * ID值
     */
    private String valueId;

    /**
     * 关联特殊权限
     */
    private String dataPermissionCode;

    /**
     * 关联角色id
     */
    private Long roleId;


}
