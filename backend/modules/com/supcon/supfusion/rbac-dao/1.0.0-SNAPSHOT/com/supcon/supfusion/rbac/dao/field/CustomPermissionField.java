package com.supcon.supfusion.rbac.dao.field;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;

/**
 * <p>
 * 自定义权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
public class CustomPermissionField extends LogicDeleteBaseEntity {


    /**
     * 编码
     */
    public static String code="CODE";

    /**
     * 模式DEV或PRODUCT 默认PRODUCT
     */
    public static String ecEnv="EC_ENV";

    /**
     * 版本
     */
    public static String version="VERSION";

    /**
     * 实体编码
     */
    public static String entityCode="ENTITY_CODE";

    /**
     * 模块编码
     */
    public static String moduleCode="MODULE_CODE";

    /**
     * 备注
     */
    public static String memo="MEMO";

    /**
     * 标题
     */
    public static String title="TITLE";

    /**
     * 条件SQL
     */
    public static String conditionSql="CONDITION_SQL";

    /**
     * JSON条件
     */
    public static String jsonCondition="JSON_CONDITION";

    /**
     * 视图编码
     */
    public static String viewCode="VIEW_CODE";


}
