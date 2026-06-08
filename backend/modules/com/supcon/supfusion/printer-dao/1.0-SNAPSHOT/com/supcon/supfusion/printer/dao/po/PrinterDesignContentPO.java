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
@TableName(value = PrinterDesignContentPO.TABLE_NAME, autoResultMap = true)
public class PrinterDesignContentPO extends BaseEntity {

    /**
     * po对应的表名
     */
    public static final String TABLE_NAME = "printer_design_content";

    /**
     * 主键id
     */
/*    @TableField(jdbcType = JdbcType.BIGINT)
    private Long id;*/

    /**
     * 模板id
     */
    @TableField(jdbcType = JdbcType.BIGINT)
    private Long templateId;

    /**
     * 设计模板json内容
     */
    private String content;

    /**
     * 是否有效
     */
    private Boolean valid = true;
}
