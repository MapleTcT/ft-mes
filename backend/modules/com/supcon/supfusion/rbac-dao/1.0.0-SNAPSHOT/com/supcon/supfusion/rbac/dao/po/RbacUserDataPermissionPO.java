package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
    * 数据分组用户权限
    * </p>
 *
 * @author panzk
 * @since 2021-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "rbac_user_data_permission", autoResultMap=true)
public class RbacUserDataPermissionPO extends LogicDeleteBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 授权方式：角色(0) 用户(1)
     */
    private Integer purviewType;


    public static String getIdFieldName() {
        return "id";
    }
    public static String getUserIdFieldName() {
        return "user_id";
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
    public static String getRoleIdFieldName() {
        return "role_id";
    }
    public static String getPurviewTypeFieldName() {
        return "purview_type";
    }

}
