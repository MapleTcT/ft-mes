package com.supcon.supfusion.organization.dao.po.person;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

/**
 * 岗位人员关系类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = OrganizationManagerPO.TABLE_NAME, autoResultMap = true)
public class OrganizationManagerPO extends BaseEntity {

    public static final String TABLE_NAME = "org_manager";

    /**
     * 主键id
     */
    private Long id;

    /**
     * 组织id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long orgId;

    /**
     * 负责人id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long managerId;

    /**
     * 负责人name
     */
    private String managerName;

    /**
     * 负责人类型,部门/岗位/组
     */
    private String managerType;
}
