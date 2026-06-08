package com.supcon.supfusion.printer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.printer.service.bo.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 打印模板service
 */
public interface PrinterTemplateService {
    /**
     * 打印模板编号是否存在
     * @param templateCode
     */
    Integer templateCodeCount(String templateCode);
    /**
     * 打印模板ADD
     * @param printerTemplateAddBO
     */
    Long addPrinterTemplate(PrinterTemplateAddBO printerTemplateAddBO);

    /**
     * 打印模板UPDATE
     * @param printerTemplateUpdateBO
     */
    void updatePrinterTemplate(PrinterTemplateUpdateBO printerTemplateUpdateBO);

    /**
     * 打印模板COPY
     * @param printerTemplateUpdateBO
     */
    void copyPrinterTemplate(PrinterTemplateUpdateBO printerTemplateUpdateBO);

    /**
     * 打印模板批量DELETE
     * @param templateIds
     */
    void deleteBatchPrinterTemplates(List<Long> templateIds);

    /**
     * 打印模板列表QUERY
     * @param printerTemplatePageQueryBO
     */
    Page queryPrinterTemplateListByAppId(PrinterTemplatePageQueryBO printerTemplatePageQueryBO);

    /**
     * 打印模板QUERY
     * @param templateId
     */
    PrinterTemplateUpdateBO queryPrinterTemplateListByTemplateId(Long templateId);

    /**
     * 保存设计模板json内容
     * @param printerDesignContentBO
     */
    void saveTemplateDesignContent(PrinterDesignContentBO printerDesignContentBO) throws IOException;

    /**
     * 加载设计模板json内容
     * @param templateId 模板id
     * @return
     */
    PrinterDesignContentBO loadTemplateDesignContent(Long templateId) throws UnsupportedEncodingException;

    void batchUpdateTemplateStatus(PrinterTemplateBatchUpdateBO printerTemplateBatchUpdateBO);
}
