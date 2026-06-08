package com.supcon.supfusion.printer.service.bo;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterRegisterBO {

    /**
     * 数据来源
     */
    private Integer source;

    /**
     * 服务地址
     */
    private String serviceUrl;

    /**
     * 服务类型
     */
    private Integer serviceType;

    private Integer callType;
}
