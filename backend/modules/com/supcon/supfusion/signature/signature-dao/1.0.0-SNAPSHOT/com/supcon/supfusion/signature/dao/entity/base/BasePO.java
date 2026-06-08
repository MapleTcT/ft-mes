package com.supcon.supfusion.signature.dao.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.framework.scaffold.mybatis.type.handler.UTCToStringTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

/**
 * @author zhang yafei
 */
@Data
public class BasePO  extends  AbstractEntity{

    @TableField(
            value = "create_staff_id",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.BIGINT
    )
    private Long createStaffId;
    @TableField(
            value = "create_time",
            fill = FieldFill.INSERT,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String createTime;

    @TableField(
            value = "modify_staff_id",
            fill = FieldFill.UPDATE,
            jdbcType = JdbcType.BIGINT
    )
    private Long modifyStaffId;
    @TableField(
            value = "modify_time",
            fill = FieldFill.UPDATE,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String modifyTime;

    @TableField(
            value = "delete_staff_id",
            fill = FieldFill.DEFAULT,
            jdbcType = JdbcType.BIGINT
    )
    private Long deleteStaffId;
    @TableField(
            value = "delete_time",
            fill = FieldFill.DEFAULT,
            jdbcType = JdbcType.TIMESTAMP,
            typeHandler = UTCToStringTypeHandler.class
    )
    private String deleteTime;
}
