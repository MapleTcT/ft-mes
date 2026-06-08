package com.supcon.supfusion.printer.service.bo;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagePrinterTemplateResultBO {

    /**
     * 打印模板id
     */
    private Long id;

    /**
     * 打印模板名称
     */
    private String name;

    /**
     * 打印模板编码
     */
    private String code;
}
