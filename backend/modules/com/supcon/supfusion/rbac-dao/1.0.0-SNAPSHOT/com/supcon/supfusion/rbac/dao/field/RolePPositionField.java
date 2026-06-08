package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 角色指定岗位表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
public class RolePPositionField {


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
     * 岗位ID
     */
    public static String positionId="POSITION_ID";

    /**
     * 角色权限ID
     */
    public static String rolepermissionId="ROLEPERMISSION_ID";


}
