package com.supcon.supfusion.organization.dao.po.company;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = CompanyPersonPO.TABLE_NAME, autoResultMap = true)
public class CompanyPersonPO extends BaseEntity {

    public static final String TABLE_NAME = "org_person_company";

    /**
     * 主键id
     */
    private Long id;

    /**
     * 公司id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long companyId;

    /**
     * 人员id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long personId;

    /**
     * 岗位id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long positionId;
    /**
     * 是否有效
     */
    private Boolean valid = true;
}