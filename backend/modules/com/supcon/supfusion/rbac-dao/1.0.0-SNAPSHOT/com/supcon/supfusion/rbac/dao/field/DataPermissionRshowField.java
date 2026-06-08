package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * <p>
 * 业务数据权限角色权限配置临时表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public class DataPermissionRshowField implements Serializable {

    /**
     * 主键ID
     */
    public static String id = "ID";

    /**
     * 版本
     */
    public static String version = "VERSION";

    /**
     * 删除者
     */
    public static String deleteStaffId = "DELETE_STAFF_ID";

    /**
     * 修改者
     */
    public static String modifyStaffId = "MODIFY_STAFF_ID";

    /**
     * 创建者
     */
    public static String createStaffId = "CREATE_STAFF_ID";


    /**
     * 是否启用
     */
    public static String isAssigned = "IS_ASSIGNED";

    public static String layRec = "LAY_REC";

    /**
     * 是否包含上下级
     */
    public static String isIncludeSub = "IS_INCLUDE_SUB";

    /**
     * 操作ID
     */
    public static String operateId = "OPERATE_ID";

    /**
     * 编码值
     */
    public static String valueCode = "VALUE_CODE";

    /**
     * 标题值
     */
    public static String valueTitle = "VALUE_TITLE";

    /**
     * ID值
     */
    public static String valueId = "VALUE_ID";

    /**
     * 关联特殊权限
     */
    public static String dataPermissionCode = "DATA_PERMISSION_CODE";

    /**
     * 关联角色id
     */
    public static String roleId = "ROLE_ID";


}
