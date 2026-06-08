package com.supcon.supfusion.printer.service.impl;

import com.supcon.supfusion.printer.config.NacosInstance;
import com.supcon.supfusion.printer.service.PrinterAppDataService;
import com.supcon.supfusion.printer.service.PrinterRegisterService;
import com.supcon.supfusion.printer.service.bo.AppDataResponseBO;
import com.supcon.supfusion.printer.service.bo.PageDataBO;
import com.supcon.supfusion.printer.service.bo.PageDataQueryBO;
import com.supcon.supfusion.printer.service.bo.PrinterRegisterBO;
import com.supcon.supfusion.printer.service.enums.ServiceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Slf4j
@Service
public class PrinterAppDataServiceImpl implements PrinterAppDataService {
    @Autowired
    @Qualifier("restTemplateClient")
    private RestTemplate restTemplate;

    @Autowired
    private NacosInstance nacosInstance;

    @Autowired
    private PrinterRegisterService printerRegisterService;

    @Override
    public List<PageDataBO> getAppList(PageDataQueryBO pageDataQueryBO) {
        PrinterRegisterBO printerRegisterBO = new PrinterRegisterBO();
        printerRegisterBO.setSource(pageDataQueryBO.getSource());
        printerRegisterBO.setServiceType(ServiceTypeEnum.APP.getCode());
        PrinterRegisterBO printerRegisterBOResult = printerRegisterService.queryPrinterRegister(printerRegisterBO);
        String url = nacosInstance.selectOneHealthyInstance(printerRegisterBOResult.getSource(), printerRegisterBOResult.getServiceUrl());
        if (StringUtils.isBlank(url)) {
            return null;
        }
        AppDataResponseBO responseBO = restTemplate.getForObject(url, AppDataResponseBO.class);
        List<PageDataBO> pageDataBOList = responseBO.getData().getData();
        return pageDataBOList;
    }

    @Override
    public List<PageDataBO> getPageList(PageDataQueryBO pageDataQueryBO) {
        PrinterRegisterBO printerRegisterBO = new PrinterRegisterBO();
        printerRegisterBO.setSource(pageDataQueryBO.getSource());
        printerRegisterBO.setServiceType(ServiceTypeEnum.PAGE.getCode());
        PrinterRegisterBO printerRegisterBOResult = printerRegisterService.queryPrinterRegister(printerRegisterBO);
        String url = nacosInstance.selectOneHealthyInstance(printerRegisterBOResult.getSource(), printerRegisterBOResult.getServiceUrl());
        if (StringUtils.isBlank(url)) {
            return null;
        }
        AppDataResponseBO responseBO = restTemplate.getForObject(url+"?entityCode=" + pageDataQueryBO.getPCode(), AppDataResponseBO.class);
        log.info("********* AppDataResponseBO ********* : {}",responseBO);
        List<PageDataBO> pageDataBOList = responseBO.getData().getData();
        return pageDataBOList;
    }
}
