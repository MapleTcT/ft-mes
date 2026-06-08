package com.supcon.supfusion.printer.service;

import com.supcon.supfusion.printer.service.bo.PrinterLabelBO;
import com.supcon.supfusion.printer.service.bo.PrinterTemplateAddBO;
import com.supcon.supfusion.printer.service.bo.PrinterTemplateUpdateBO;

import java.util.List;

/**
 * 打印模板service
 */
public interface PrinterLabelService {
    /**
     * 标签ADD
     * @param printerLabelBO
     */
    void addPrinterLabel(PrinterLabelBO printerLabelBO);


    /**
     * 标签列表QUERY
     */
    List<PrinterLabelBO> queryPrinterLabelList();
}
