package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 业务数据权限角色关联表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public class RoleDataPermissionField implements Serializable {


    /**
     * 主键ID
     */
    public static String id = "ID";

    /**
     * 版本
     */
    public static String version="VERSION" ;

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
     * 角色权限ID
     */
    public static String rolepermissionId="ROLEPERMISSION_ID";


}
