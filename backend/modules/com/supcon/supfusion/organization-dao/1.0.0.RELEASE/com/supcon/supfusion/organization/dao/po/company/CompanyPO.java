package com.supcon.supfusion.organization.dao.po.company;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

/**
 * 公司PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = CompanyPO.TABLE_NAME, autoResultMap = true)
public class CompanyPO extends BaseEntity {

    public static final String TABLE_NAME = "org_company";

    /**
     * 公司id
     */
    private Long id;

    /**
     * 版本号
     */
    private Long rowVersion;

    /**
     * 公司编码
     */
    private String code;

    /**
     * 对应旧版本的公司的name
     */
    private String oldId;

    /**
     * 描述
     */
    private String description;

    /**
     * 集团或公司简称
     */
    private String shortName;

    /**
     * 集团或公司全称
     */
    private String fullName;

    /**
     * 公司全路径
     */
    private String fullPath;

    /**
     * 公司id全路径
     */
    private String layRec;

    /**
     * 集团或公司地址
     */
    private String address;

    /**
     * 节点层级
     */
    @TableField(jdbcType = JdbcType.INTEGER)
    private Integer layNo;

    /**
     * 同层级下节点顺序
     */
    private Double sort;

    /**
     * 父级节点id
     */
    @TableField(value = "parent_id", updateStrategy = FieldStrategy.IGNORED, jdbcType = JdbcType.BIGINT)
    private Long parentId;

    /**
     * 是否有效
     */
    private Boolean valid = true;


}
