package com.supcon.supfusion.printer.service;

import com.supcon.supfusion.printer.service.bo.PagePrinterTemplateResultBO;

import java.util.List;

public interface PrinterService {

    /**
     * 根据页面id查询打印模板列表
     * @param pageId 页面id
     * @return
     */
    List<PagePrinterTemplateResultBO> listTemplatesByPageId(String pageId);
}
