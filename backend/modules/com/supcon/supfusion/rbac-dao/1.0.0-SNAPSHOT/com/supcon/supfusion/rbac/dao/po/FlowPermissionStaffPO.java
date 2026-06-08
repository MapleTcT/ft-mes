package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
@TableName(value = "rbac_flow_permission_staff", autoResultMap=true)
public class FlowPermissionStaffPO implements Serializable {

    private static final long serialVersionUID = 6021698856953698360L;

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
     * 人员ID
     */
    @TableField("staff_id")
    private Long staffId;

    /**
     * 权限ID
     */
    @TableField("flowpermission_id")
    private Long flowpermissionId;


}
