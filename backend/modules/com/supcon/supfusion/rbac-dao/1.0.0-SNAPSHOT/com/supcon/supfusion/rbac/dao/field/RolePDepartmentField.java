package com.supcon.supfusion.rbac.dao.field;

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
public class RolePDepartmentField{


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本
     */
    public static String version="VERSION";

    /**
     * 包含下级
     */
    public static String includeLower="INCLUDE_LOWER";

    /**
     * 部门ID
     */
    public static String departmentId="DEPARTMENT_ID";

    /**
     * 角色权限ID
     */
    public static String rolepermissionId="ROLEPERMISSION_ID";

}
