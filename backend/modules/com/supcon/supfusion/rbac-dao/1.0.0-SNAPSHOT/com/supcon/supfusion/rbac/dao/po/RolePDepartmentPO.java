package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Data
@TableName(value = "rbac_rolepdepartment", autoResultMap=true)
public class RolePDepartmentPO implements Serializable {


    private static final long serialVersionUID = 709681778229794643L;
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
     * 部门ID
     */
    @TableField("DEPARTMENT_ID")
    private Long departmentId;

    /**
     * 角色权限ID
     */
    @TableField("ROLEPERMISSION_ID")
    private Long rolepermissionId;

}
