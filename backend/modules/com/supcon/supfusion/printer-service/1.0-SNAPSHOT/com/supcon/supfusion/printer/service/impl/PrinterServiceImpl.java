package com.supcon.supfusion.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterTemplateMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterTemplateRelationPageMapper;
import com.supcon.supfusion.printer.dao.po.PrinterTemplatePO;
import com.supcon.supfusion.printer.dao.po.PrinterTemplateRelationPagePO;
import com.supcon.supfusion.printer.service.PrinterService;
import com.supcon.supfusion.printer.service.bo.PagePrinterTemplateResultBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PrinterServiceImpl implements PrinterService {

    @Autowired
    private PrinterTemplateRelationPageMapper printerTemplateRelationPageMapper;

    @Autowired
    private PrinterTemplateMapper printerTemplateMapper;

    /**
     * 根据页面id查询打印模板列表
     * @param pageId 页面id
     * @return
     */
    @Override
    public List<PagePrinterTemplateResultBO> listTemplatesByPageId(String pageId) {

        QueryWrapper<PrinterTemplateRelationPagePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrinterTemplateRelationPagePO :: getPageId, pageId);
        List<PrinterTemplateRelationPagePO> printerTemplateRelationPagePOS = printerTemplateRelationPageMapper.selectList(queryWrapper);
        if (printerTemplateRelationPagePOS == null || printerTemplateRelationPagePOS.size() == 0) {
            return null;
        }

        List<Long> templateIds = new ArrayList<>();
        printerTemplateRelationPagePOS.stream().forEach(item -> templateIds.add(item.getTemplateId()));

        QueryWrapper<PrinterTemplatePO> templatePOQueryWrapper = new QueryWrapper<>();
        templatePOQueryWrapper.lambda().in(PrinterTemplatePO :: getId, templateIds).eq(PrinterTemplatePO::getEnabled, 1).eq(PrinterTemplatePO::getValid, true);
        List<PrinterTemplatePO> templatePOS = printerTemplateMapper.selectList(templatePOQueryWrapper);
        if (templatePOS == null || templatePOS.size() == 0) {
            return null;
        }
        List<PagePrinterTemplateResultBO> list = new ArrayList<>();
        templatePOS.stream().forEach(item -> {
            PagePrinterTemplateResultBO pagePrinterTemplateResultBO = new PagePrinterTemplateResultBO();
            pagePrinterTemplateResultBO.setId(item.getId());
            pagePrinterTemplateResultBO.setCode(item.getTemplateCode());
            pagePrinterTemplateResultBO.setName(item.getTemplateName());
            list.add(pagePrinterTemplateResultBO);
        });
        return list;
    }
}
