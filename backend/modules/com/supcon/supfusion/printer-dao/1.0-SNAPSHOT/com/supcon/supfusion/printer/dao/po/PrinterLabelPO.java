package com.supcon.supfusion.printer.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 打印标签表entity
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PrinterLabelPO.TABLE_NAME, autoResultMap = true)
public class PrinterLabelPO extends BaseEntity {
    /**
     * po对应的表名
     */
    public static final String TABLE_NAME = "printer_label";

    /**
     * 主键
     */
    private Long id;

    /**
     * 标签名称
     */
    private String labelName;
}
