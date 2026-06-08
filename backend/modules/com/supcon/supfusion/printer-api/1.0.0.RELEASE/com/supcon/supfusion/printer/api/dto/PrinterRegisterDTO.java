package com.supcon.supfusion.printer.api.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterRegisterDTO {
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

    /**
     * http请求方式
     */
    private Integer callType;
}
