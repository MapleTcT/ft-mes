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
public class UserPPositionField implements Serializable {


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
     * 用户权限ID
     */
    public static String userpermissionId="USERPERMISSION_ID";


}
