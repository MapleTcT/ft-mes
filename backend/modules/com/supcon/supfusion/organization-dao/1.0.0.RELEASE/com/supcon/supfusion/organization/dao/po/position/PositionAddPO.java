package com.supcon.supfusion.organization.dao.po.position;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

/**
 * 岗位PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PositionAddPO.TABLE_NAME, autoResultMap = true)
public class PositionAddPO extends BaseEntity {

    public static final String TABLE_NAME = "org_position";

    /**
     * 岗位id
     */
    private Long id;
    /**
     * 岗位编码
     */
    private String code;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 对应老版本的name别名
     */
    private String oldId;


    /**
     * 所属公司id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long companyId;

    /**
     * 关联的部门id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long depId;

    /**
     * 上级岗位id（如果上级是公司则为空）
     */
    @TableField(value = "parent_id", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.BIGINT)
    private Long parentId;

    /**
     * 描述
     */
    @TableField(value = "description", updateStrategy = FieldStrategy.NOT_NULL)
    private String description;

    /**
     * 岗位层级
     */
    private Integer layNo;

    /**
     * 岗位全路径
     */
    private String fullPath;

    /**
     * 岗位id全路径
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
