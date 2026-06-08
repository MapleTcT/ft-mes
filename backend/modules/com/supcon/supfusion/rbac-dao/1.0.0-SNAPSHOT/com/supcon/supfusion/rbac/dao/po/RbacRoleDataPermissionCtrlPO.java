package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
    * 资源集角色是否受控
    * </p>
 *
 * @author panzk
 * @since 2021-01-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "rbac_role_data_permission_ctrl", autoResultMap=true)
public class RbacRoleDataPermissionCtrlPO extends BaseEntity {

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
     * 资源编码
     */
    private String groupCode;

    /**
     * group_code资源是否受控 0 不受控　1 受控
     */
    private Integer controlled;


    public static String getIdFieldName() {
        return "id";
    }
    public static String getRoleIdFieldName() {
        return "role_id";
    }
    public static String getCidFieldName() {
        return "cid";
    }
    public static String getGroupCodeFieldName() {
        return "group_code";
    }
    public static String getControlledFieldName() {
        return "controlled";
    }

}
