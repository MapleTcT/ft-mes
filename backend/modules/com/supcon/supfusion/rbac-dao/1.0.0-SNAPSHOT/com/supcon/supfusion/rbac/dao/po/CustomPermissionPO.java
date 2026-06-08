package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 自定义权限表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_custom_permission", autoResultMap=true)
public class CustomPermissionPO extends LogicDeleteBaseEntity {


    private static final long serialVersionUID = -4057641536265766642L;
    /**
     * 编码
     */
    @TableId("CODE")
    private String code;

    /**
     * 模式DEV或PRODUCT 默认PRODUCT
     */
    @TableField("EC_ENV")
    private String ecEnv;

    /**
     * 版本
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 实体编码
     */
    @TableField("ENTITY_CODE")
    private String entityCode;

    /**
     * 模块编码
     */
    @TableField("MODULE_CODE")
    private String moduleCode;

    /**
     * 备注
     */
    @TableField("MEMO")
    private String memo;

    /**
     * 标题
     */
    @TableField("TITLE")
    private String title;

    /**
     * 条件SQL
     */
    @TableField("CONDITION_SQL")
    private String conditionSql;

    /**
     * JSON条件
     */
    @TableField("JSON_CONDITION")
    private String jsonCondition;

    /**
     * 视图编码
     */
    @TableField("VIEW_CODE")
    private String viewCode;


}
