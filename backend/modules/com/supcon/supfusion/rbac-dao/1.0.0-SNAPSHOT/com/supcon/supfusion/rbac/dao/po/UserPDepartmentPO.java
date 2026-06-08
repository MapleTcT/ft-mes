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
@TableName(value = "rbac_userpdepartment", autoResultMap=true)
public class UserPDepartmentPO implements Serializable {


    private static final long serialVersionUID = 25794207371947107L;
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
     * 用户权限ID
     */
    @TableField("USERPERMISSION_ID")
    private Long userpermissionId;

}
