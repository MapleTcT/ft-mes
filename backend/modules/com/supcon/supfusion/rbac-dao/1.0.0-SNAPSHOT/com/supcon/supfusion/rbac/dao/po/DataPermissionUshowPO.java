package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 业务数据权限用户权限配置临时表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@TableName(value = "rbac_data_permission_ushow", autoResultMap=true)
public class DataPermissionUshowPO extends BaseEntity implements Serializable {


    private static final long serialVersionUID = -6867713243133548227L;
    /**
     * 主键ID
     */
    @TableId("ID")
    private Long id;

    /**
     * 版本
     */
    @TableField("VERSION")
    private Integer version;


    /**
     * 是否启用
     */
    @TableField("IS_ASSIGNED")
    private Boolean isAssigned;

    @TableField("LAY_REC")
    private String layRec;

    /**
     * 是否包含上下级
     */
    @TableField("IS_INCLUDE_SUB")
    private String isIncludeSub;

    /**
     * 操作ID
     */
    @TableField("OPERATE_ID")
    private Long operateId;

    /**
     * 编码值
     */
    @TableField("VALUE_CODE")
    private String valueCode;

    /**
     * 标题值
     */
    @TableField("VALUE_TITLE")
    private String valueTitle;

    /**
     * ID值
     */
    @TableField("VALUE_ID")
    private String valueId;

    /**
     * 关联特殊权限
     */
    @TableField("DATA_PERMISSION_CODE")
    private String dataPermissionCode;

    /**
     * 关联用户ID
     */
    @TableField("USER_ID")
    private Long userId;


}
