package com.supcon.supfusion.printer.interapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PagePrinterTemplateResultVO extends VO {

    /**
     * 模板id
     */
    private Long id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板编码
     */
    private String code;
}
