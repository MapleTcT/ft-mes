package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
    * 数据分组角色权限
    * </p>
 *
 * @author panzk
 * @since 2021-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "rbac_role_data_permission", autoResultMap=true)
public class RbacRoleDataPermissionPO extends LogicDeleteBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 公司ID
     */
    private Long cid;

    /**
     * 业务数据编码
     */
    private String resourceCode;

    /**
     * 业务数据名称
     */
    private String resourceName;

    /**
     * 业务数据类型
     */
    private String resourceType;

    /**
     * 资源编码
     */
    private String groupCode;


    public static String getIdFieldName() {
        return "id";
    }
    public static String getRoleIdFieldName() {
        return "role_id";
    }
    public static String getCidFieldName() {
        return "cid";
    }
    public static String getResourceCodeFieldName() {
        return "resource_code";
    }
    public static String getResourceNameFieldName() {
        return "resource_name";
    }
    public static String getResourceTypeFieldName() {
        return "resource_type";
    }
    public static String getGroupCodeFieldName() {
        return "group_code";
    }

}
