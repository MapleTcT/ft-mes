package com.supcon.supfusion.printer.service.rpc;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.printer.api.PrinterApiService;
import com.supcon.supfusion.printer.api.dto.EntityDataResultDTO;
import com.supcon.supfusion.printer.api.dto.EntityQueryConditionDTO;
import com.supcon.supfusion.printer.api.dto.PrinterLogDTO;
import com.supcon.supfusion.printer.api.dto.PrinterRegisterDTO;
import com.supcon.supfusion.printer.service.DataSourceService;
import com.supcon.supfusion.printer.service.PrinterLogService;
import com.supcon.supfusion.printer.service.PrinterRegisterService;
import com.supcon.supfusion.printer.service.bo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * rpc实现
 */
@ServiceApiService
public class PrinterApiServiceImpl extends BaseController implements PrinterApiService {

    @Autowired
    private PrinterLogService printerLogService;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private PrinterRegisterService printerRegisterService;
/*    @Override
    public void addDepartment(PrinterLogDTO printerLogDTO) {

        PrinterLogBO printerLogBO = new PrinterLogBO();
        BeanUtils.copyProperties(printerLogDTO, printerLogBO);
        printerLogService.addPrinterLog(printerLogBO);
    }*/

    @Override
    public Boolean register(PrinterRegisterDTO printerRegisterDTO) {
        PrinterRegisterBO printerRegisterBO = new PrinterRegisterBO();
        BeanUtils.copyProperties(printerRegisterDTO, printerRegisterBO);
        printerRegisterService.addPrinterRegister(printerRegisterBO);
        return true;
    }
}
