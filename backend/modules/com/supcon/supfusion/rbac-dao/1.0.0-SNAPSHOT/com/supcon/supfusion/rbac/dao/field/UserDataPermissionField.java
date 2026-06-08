package com.supcon.supfusion.rbac.dao.field;

import java.io.Serializable;

/**
 * <p>
 * 业务数据权限用户关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public class UserDataPermissionField implements Serializable {


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本
     */
    public static String version="VERSION";

    /**
     * 配置内容
     */
    public static String configString="CONFIG_STRING";

    /**
     * SQL内容
     */
    public static String content="CONTENT";

    /**
     * 其他限制编码
     */
    public static String dataPermissionCode="DATA_PERMISSION_CODE";

    /**
     * 用户权限ID
     */
    public static String userpermissionId="USERPERMISSION_ID";


}
