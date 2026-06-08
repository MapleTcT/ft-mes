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
 * 角色指定人员
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
@TableName(value = "rbac_rolepstaff", autoResultMap=true)
public class RolePStaffPO implements Serializable {


    private static final long serialVersionUID = -1671687051669039131L;
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
     * 人员ID
     */
    @TableField("STAFF_ID")
    private Long staffId;

    /**
     * 角色权限ID
     */
    @TableField("ROLEPERMISSION_ID")
    private Long rolepermissionId;


}
