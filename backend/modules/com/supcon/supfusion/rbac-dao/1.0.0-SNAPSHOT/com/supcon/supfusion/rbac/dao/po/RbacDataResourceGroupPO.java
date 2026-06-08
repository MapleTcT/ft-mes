package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
    * 
    * </p>
 *
 * @author panzk
 * @since 2021-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "rbac_data_resource_group", autoResultMap=true)
public class RbacDataResourceGroupPO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 资源编码
     */
    private String groupCode;

    /**
     * 资源名称
     */
    private String groupName;

    /**
     * 资源获取地址
     */
    private String resourceUrl;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 公司ID
     */
    private Long cid;


    public static String getIdFieldName() {
        return "id";
    }
    public static String getGroupCodeFieldName() {
        return "group_code";
    }
    public static String getGroupNameFieldName() {
        return "group_name";
    }
    public static String getResourceUrlFieldName() {
        return "resource_url";
    }
    public static String getModuleCodeFieldName() {
        return "module_code";
    }
    public static String getCidFieldName() {
        return "cid";
    }

}
