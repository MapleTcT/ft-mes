package com.supcon.supfusion.printer.service.bo;

import lombok.*;

/**
 * 分页查询模板列表
 * @author yuyimao
 * @date 2020/10/16 5:01 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterTemplatePageQueryBO {

    /**
     * 模板编号
     */
    private String appId;

    /**
     * 页面大小
     */
    private Integer pageSize;

    /**
     * 页面编号
     */
    private Integer pageNum;
}
