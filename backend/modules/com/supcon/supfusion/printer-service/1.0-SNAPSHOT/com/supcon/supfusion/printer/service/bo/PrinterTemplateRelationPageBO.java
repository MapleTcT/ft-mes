package com.supcon.supfusion.printer.service.bo;

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
public class PrinterTemplateRelationPageBO{

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
