package com.supcon.supfusion.printer.dao.po;

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
@TableName(value = PrinterLogPO.TABLE_NAME, autoResultMap = true)
public class PrinterLogPO extends BaseEntity {

    /**
     * po对应的表名
     */
    public static final String TABLE_NAME = "printer_log";

    /**
     * 主键id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long id;

    /**
     * 打印模板id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long templateId;

    /**
     * 页面id
     */
    private String pageId;
}
