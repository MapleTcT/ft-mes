package com.supcon.supfusion.organization.dao.po.company;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

/**
 * 公司标签PO类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = CompanyTagPO.TABLE_NAME, autoResultMap = true)
public class CompanyTagPO extends BaseEntity {

    public static final String TABLE_NAME = "org_tag";

    /**
     * 公司id
     */
    private Long id;

    /**
     * 版本号
     */
    private Long rowVersion;

    /**
     * 标签类型
     */
    @TableField(value = "tag_type")
    private String type = "Company";

    /**
     * 标签名称
     */
    private String name;

    /**
     * 公司id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long companyId;
}
