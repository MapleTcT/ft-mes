package com.supcon.supfusion.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.printer.dao.mapper.PrinterLabelMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterTemplateMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterTemplateRelationPageMapper;
import com.supcon.supfusion.printer.dao.po.PrinterLabelPO;
import com.supcon.supfusion.printer.dao.po.PrinterTemplatePO;
import com.supcon.supfusion.printer.dao.po.PrinterTemplateRelationPagePO;
import com.supcon.supfusion.printer.service.PrinterLabelService;
import com.supcon.supfusion.printer.service.PrinterTemplateService;
import com.supcon.supfusion.printer.service.bo.PrinterLabelBO;
import com.supcon.supfusion.printer.service.bo.PrinterTemplateAddBO;
import com.supcon.supfusion.printer.service.bo.PrinterTemplateRelationPageBO;
import com.supcon.supfusion.printer.service.bo.PrinterTemplateUpdateBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class PrinterLabelServiceImpl extends ServiceImpl<PrinterLabelMapper, PrinterLabelPO> implements PrinterLabelService {

    @Override
    public void addPrinterLabel(PrinterLabelBO printerLabelBO) {
        PrinterLabelPO printerLabelPO = new PrinterLabelPO();
        BeanUtils.copyProperties(printerLabelBO,printerLabelPO);
        save(printerLabelPO);
    }

    @Override
    public List<PrinterLabelBO> queryPrinterLabelList() {
        List<PrinterLabelPO> printerLabelPOList = list();
        List<PrinterLabelBO> printerLabelBOList = new ArrayList<>();
        printerLabelPOList.forEach(p -> {
            PrinterLabelBO printerLabelBO = new PrinterLabelBO();
            BeanUtils.copyProperties(p,printerLabelBO);
            printerLabelBOList.add(printerLabelBO);
        });
        return printerLabelBOList;
    }
}
