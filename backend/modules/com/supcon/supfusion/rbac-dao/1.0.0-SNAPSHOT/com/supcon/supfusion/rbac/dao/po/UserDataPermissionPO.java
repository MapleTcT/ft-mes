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
 * 业务数据权限用户关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@TableName(value = "rbac_user_datapermission", autoResultMap=true)
public class UserDataPermissionPO extends BaseEntity implements Serializable {


    private static final long serialVersionUID = 7279526702681523667L;
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
     * 配置内容
     */
    @TableField("CONFIG_STRING")
    private String configString;

    /**
     * SQL内容
     */
    @TableField("CONTENT")
    private String content;

    /**
     * 其他限制编码
     */
    @TableField("DATA_PERMISSION_CODE")
    private String dataPermissionCode;

    /**
     * 用户权限ID
     */
    @TableField("USERPERMISSION_ID")
    private Long userpermissionId;


}
