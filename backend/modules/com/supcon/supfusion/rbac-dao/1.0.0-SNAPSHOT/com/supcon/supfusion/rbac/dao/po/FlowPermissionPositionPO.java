package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 标签表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
@Data
@TableName(value = "rbac_flow_permission_position", autoResultMap=true)
public class FlowPermissionPositionPO implements Serializable {

    private static final long serialVersionUID = 8225226074588398949L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 版本号
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 岗位ID
     */
    @TableField("position_id")
    private Long positionId;

    /**
     * 是否包含下级
     */
    @TableField("include_lower")
    private Boolean includeLower;

    /**
     * 权限ID
     */
    @TableField("flowpermission_id")
    private Long flowpermissionId;


}
