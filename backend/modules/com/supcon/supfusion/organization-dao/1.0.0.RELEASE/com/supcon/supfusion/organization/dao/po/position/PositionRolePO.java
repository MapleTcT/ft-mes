package com.supcon.supfusion.organization.dao.po.position;

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
@TableName(value = PositionRolePO.TABLE_NAME, autoResultMap = true)
public class PositionRolePO extends BaseEntity {

    public static final String TABLE_NAME = "org_position_role";

    /**
     * 主键id
     */
    private Long id;

    /**
     * 岗位id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long positionId;

    /**
     * 角色id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long roleId;
}
