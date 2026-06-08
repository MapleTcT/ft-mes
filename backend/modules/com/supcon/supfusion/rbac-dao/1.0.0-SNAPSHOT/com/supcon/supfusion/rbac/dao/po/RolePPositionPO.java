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
 * 角色指定岗位表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
@TableName(value = "rbac_rolepposition", autoResultMap=true)
public class RolePPositionPO implements Serializable {


    private static final long serialVersionUID = -2508515508605637955L;
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
     * 包含下级
     */
    @TableField("INCLUDE_LOWER")
    private Boolean includeLower;

    /**
     * 岗位ID
     */
    @TableField("POSITION_ID")
    private Long positionId;

    /**
     * 角色权限ID
     */
    @TableField("ROLEPERMISSION_ID")
    private Long rolepermissionId;


}
