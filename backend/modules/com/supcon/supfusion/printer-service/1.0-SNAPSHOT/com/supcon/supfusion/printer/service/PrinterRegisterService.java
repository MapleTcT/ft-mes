package com.supcon.supfusion.printer.service;

import com.supcon.supfusion.printer.service.bo.EntityPageUrlBO;
import com.supcon.supfusion.printer.service.bo.PrinterRegisterBO;


/**
 * 数据注册service
 */
public interface PrinterRegisterService {

    /**
     * 数据注册
     * @param printerRegisterBO
     */
    void addPrinterRegister(PrinterRegisterBO printerRegisterBO);


    /**
     * 注册信息QUERY
     */
    PrinterRegisterBO queryPrinterRegister(PrinterRegisterBO printerRegisterBO);

    /**
     * 实体对象注册页面地址接口
     * @param entityPageUrlBO
     */
    void registerEntityPageUrl(EntityPageUrlBO entityPageUrlBO);
}
