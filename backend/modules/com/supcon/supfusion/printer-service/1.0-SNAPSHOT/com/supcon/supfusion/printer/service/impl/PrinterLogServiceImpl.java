package com.supcon.supfusion.printer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.printer.dao.mapper.PrinterLogMapper;
import com.supcon.supfusion.printer.dao.po.PrinterLogPO;
import com.supcon.supfusion.printer.service.PrinterLogService;
import com.supcon.supfusion.printer.service.bo.PrinterLogBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PrinterLogServiceImpl extends ServiceImpl<PrinterLogMapper, PrinterLogPO> implements PrinterLogService {

    /**
     * 记录打印日志
     * @param printerLogBO 日志实体
     */
    @Override
    public void addPrinterLog(PrinterLogBO printerLogBO) {
        PrinterLogPO printerLogPO = new PrinterLogPO();
        BeanUtils.copyProperties(printerLogBO, printerLogPO);
        save(printerLogPO);
    }
}
