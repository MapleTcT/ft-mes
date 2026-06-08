package com.supcon.supfusion.printer.service.bo;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterLogBO {

    /**
     * 模板id
     */
    private Long templateId;

    /**
     * 页面id
     */
    private String pageId;
}
