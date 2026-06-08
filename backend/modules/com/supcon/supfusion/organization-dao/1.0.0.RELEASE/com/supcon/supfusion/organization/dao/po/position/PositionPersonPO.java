package com.supcon.supfusion.organization.dao.po.position;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;
import org.apache.ibatis.type.JdbcType;

import java.util.Date;

/**
 * 岗位人员关系类
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PositionPersonPO.TABLE_NAME, autoResultMap = true)
public class PositionPersonPO extends BaseEntity {

    public static final String TABLE_NAME = "org_person_position";

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
     * 人员id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long personId;

    /**
     * 上岗时间
     */
    private Date workTime;

    /**
     * 离岗时间
     */
    private Date offTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否有效
     */
    private Boolean valid = true;
}
