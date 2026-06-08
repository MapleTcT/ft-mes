package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 角色表
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "rbac_role", autoResultMap = true)
public class RolePO extends LogicDeleteBaseEntity {


    private static final long serialVersionUID = -949292858320323010L;
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 公司ID
     */
    @TableField("CID")
    private Long cid;

    /**
     * 是否叶子
     */
    @TableField("LEAF")
    private Boolean leaf;

    /**
     * 层级全路径
     */
    @TableField("FULL_PATH_NAME")
    private String fullPathName;

    /**
     * 上级节点ID
     */
    @TableField("PARENT_ID")
    private Long parentId;

    /**
     * 层级
     */
    @TableField("LAY_NO")
    private Integer layNo;

    /**
     * 层级结构
     */
    @TableField("LAY_REC")
    private String layRec;

    /**
     * 用于软件公司同步接口
     */
    @TableField("UUID")
    private String uuid;

    /**
     * 三员类型:1系统管理员,2安全保密员 ,3安全审计员
     */
    @TableField("THREE_ROLE_TYPE")
    private Integer threeRoleType;

    /**
     * 角色类型
     * SystemCode:
     * ROLE_TYPE/roletype 1 默认公司
     */
    @TableField("ROLE_TYPE")
    private String roleType;

    /**
     * 排序
     */
    @TableField("SORT")
    private Double sort;

    /**
     * 描述
     */
    @TableField("DESCRIPTION")
    private String description;

    /**
     * 名称
     */
    @TableField("NAME")
    private String name;

    /**
     * 编码
     */
    @TableField("CODE")
    private String code;

    /**
     * 版本
     */
    @TableField("VERSION")
    private Integer version;

    /**
     * 父节点
     */
    @TableField(exist = false)
    private RolePO parent;

    /**
     * 标签
     */
    @TableField(exist = false)
    private List<TagPO> tags;


    public static String getCidFieldName() {
        return "CID";
    }

    public static String getValidFieldName() {
        return "VALID";
    }


    public static String getNameFieldName() {
        return "NAME";
    }

    public static String getCodeFieldName() {
        return "CODE";
    }

    public static String getCreateTimeFieldName() {
        return "CREATE_TIME";
    }
}
