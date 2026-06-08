package com.supcon.supfusion.printer.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * 打印模板页面关联表entity
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = PrinterTemplateRelationPagePO.TABLE_NAME, autoResultMap = true)
public class PrinterTemplateRelationPagePO extends BaseEntity {
    /**
     * po对应的表名
     */
    public static final String TABLE_NAME = "printer_template_relation_page";

    /**
     * 主键
     */
    private Long id;

    /**
     * 模板编号
     */
    private Long templateId;

    /**
     * 页面编号
     */
    private String pageId;

    /**
     * 模型编号
     */
    private String modelCode;
}
