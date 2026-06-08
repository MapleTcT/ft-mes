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
 * 自定义权限用户关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@TableName(value = "rbac_user_custompermission_ref", autoResultMap=true)
public class UserCustomPermissionRefPO implements Serializable {


    private static final long serialVersionUID = -7364854197631319909L;
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
     * 其他限制编码
     */
    @TableField("CUSTOM_PERMISSION_CODE")
    private String customPermissionCode;

    /**
     * 用户权限ID
     */
    @TableField("USERPERMISSION_ID")
    private Long userpermissionId;


}
