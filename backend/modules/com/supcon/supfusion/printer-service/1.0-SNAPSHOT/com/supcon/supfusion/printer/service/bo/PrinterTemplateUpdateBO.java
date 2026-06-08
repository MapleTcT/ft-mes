package com.supcon.supfusion.printer.service.bo;

import lombok.*;

import java.util.List;

/**
 * @author liyiming
 * @date 2020/10/9 3:31 下午
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrinterTemplateUpdateBO {
    /**
     * 模板编号
     */
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 国际化编号
     */
    private String i18nKey;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * app编号
     */
    private String appId;

    /**
     * 模板标签
     */
    private String labelNames;

    /**
     * 描述
     */
    private String templateDesc;

    /**
     * 启停状态
     */
    private Integer enabled;

    /**
     * 关联页面
     */
    private List<PrinterTemplateRelationPageBO> pageDatas;
}
