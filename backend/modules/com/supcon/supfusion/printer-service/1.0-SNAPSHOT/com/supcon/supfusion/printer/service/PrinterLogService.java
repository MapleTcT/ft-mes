package com.supcon.supfusion.printer.service;

import com.supcon.supfusion.printer.service.bo.PrinterLogBO;

/**
 * 打印功能日志记录service
 */
public interface PrinterLogService {

    /**
     * 打印时记录打印日志
     * @param printerLogBO
     */
    void addPrinterLog(PrinterLogBO printerLogBO);
}
