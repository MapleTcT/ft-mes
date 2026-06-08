package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 角色用户表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public class RoleUserField{


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本信息
     */
    public static String version="VERSION";

    /**
     * 是否仅是岗位带入的角色
     */
    public static String positionFlag="POSITION_FLAG";

    /**
     * 角色ID
     */
    public static String roleId="ROLE_ID";

    /**
     * 用户ID
     */
    public static String userId="USER_ID";


    /**
     * 调出时间
     */
    public static String endTime="END_TIME";

    /**
     * 调入时间
     */
    public static String startTime="START_TIME";

    /**
     * 用户名
     */
    public static String userName="USER_NAME";

    /**
     * 人员名
     */
    public static String personName="PERSON_NAME";

    /**
     * 人员编码
     */
    public static String personCode="PERSON_CODE";

    /**
     * 来源 1 来源于用户 2 来源于岗位 3 两者都有
     */
    public static String fromPosition="FROM_POSITION";

}
