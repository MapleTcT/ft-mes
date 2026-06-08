package com.supcon.supfusion.printer.service.bo;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterDesignContentBO {

    /**
     * 模板id
     */
    private Long templateId;

    /**
     * 设计模板json内容
     */
    private String content;

    /**
     * 启停状态
     */
    private Integer enabled;
}
