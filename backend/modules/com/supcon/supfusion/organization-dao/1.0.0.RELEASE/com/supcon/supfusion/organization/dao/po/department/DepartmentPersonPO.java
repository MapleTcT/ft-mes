package com.supcon.supfusion.organization.dao.po.department;

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
@TableName(value = DepartmentPersonPO.TABLE_NAME, autoResultMap = true)
public class DepartmentPersonPO extends BaseEntity {

    public static final String TABLE_NAME = "org_person_department";

    /**
     * 主键id
     */
    private Long id;

    /**
     * 部门id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long deptId;

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