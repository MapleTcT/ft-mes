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
 * 业务数据权限定义表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_data_permission", autoResultMap=true)
public class DataPermissionPO extends LogicDeleteBaseEntity {


    private static final long serialVersionUID = -434945188405503754L;
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
     * 顺序
     */
    @TableField("ORDER_NO")
    private Double orderNo;

    /**
     * 字段编码
     */
    @TableField("PROPERTY_CODE")
    private String propertyCode;

    /**
     * 关联的参照视图编码
     */
    @TableField("REF_VIEW_CODE")
    private String refViewCode;

    /**
     * 是否树结构
     */
    @TableField("IS_TREE")
    private Boolean isTree;

    /**
     * 关联模型编码
     */
    @TableField("TARGET_MODEL_CODE")
    private String targetModelCode;

    /**
     * 类型
     */
    @TableField("TYPE")
    private String type;

    /**
     * 关系
     */
    @TableField("RELATION")
    private String relation;

    /**
     * 等级
     */
    @TableField("RANK")
    private Integer rank;

    /**
     * 模型编码
     */
    @TableField("MODEL_CODE")
    private String modelCode;


}
