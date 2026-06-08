package com.supcon.supfusion.rbac.dao.field;

import java.io.Serializable;

/**
 * <p>
 * 自定义权限角色关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public class RoleCustomPermissionRefField implements Serializable {


    /**
     * 主键ID
     */
    public static String id="ID";

    /**
     * 版本
     */
    public static String version="VERSION";

    /**
     * 其他限制编码
     */
    public static String customPermissionCode="CUSTOM_PERMISSION_CODE";

    /**
     * 角色权限ID
     */
    public static String rolepermissionId="ROLEPERMISSION_ID";


}
