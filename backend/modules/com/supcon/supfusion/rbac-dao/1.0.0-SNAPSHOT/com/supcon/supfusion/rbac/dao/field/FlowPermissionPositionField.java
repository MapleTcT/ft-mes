package com.supcon.supfusion.rbac.dao.field;

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
public class FlowPermissionPositionField{

    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本号
     */
    public static String version="VERSION";

    /**
     * 岗位ID
     */
    public static String positionId="position_id";

    /**
     * 是否包含下级
     */
    public static String includeLower="include_lower";

    /**
     * 权限ID
     */
    public static String flowpermissionId="flowpermission_id";


}
