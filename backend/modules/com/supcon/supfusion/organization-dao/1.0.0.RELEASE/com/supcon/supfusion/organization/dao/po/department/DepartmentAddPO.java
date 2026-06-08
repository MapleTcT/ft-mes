package com.supcon.supfusion.organization.dao.po.department;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

/**
 * 部门PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = DepartmentAddPO.TABLE_NAME, autoResultMap = true)
public class DepartmentAddPO extends BaseEntity {

    public static final String TABLE_NAME = "org_department";

    /**
     * 部门id
     */
    private Long id;
    /**
     * 部门编码
     */
    private String code;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 老版本的别名name
     */
    private String oldId;
    /**
     * 部门类型
     */
    @TableField(value = "dept_type")
    private String type;

    /**
     * 所属公司id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long companyId;

    /**
     * 上级部门id（如果上级是公司则为空）
     */
    @TableField(value = "parent_id", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.BIGINT)
    private Long parentId;

    /**
     * 描述
     */
    @TableField(value = "description", updateStrategy = FieldStrategy.NOT_NULL)
    private String description;


    /**
     * 部门层级
     */
    private Integer layNo;

    /**
     * 部门全路径
     */
    private String fullPath;

    /**
     * 部门id全路径
     */
    private String layRec;

    /**
     * 顺序
     */
    private Double sort;

    /**
     * 是否是叶子节点０不是，１是
     */
    private Boolean leaf;

    private Boolean valid;

    private Boolean sysFlag = false;
}
