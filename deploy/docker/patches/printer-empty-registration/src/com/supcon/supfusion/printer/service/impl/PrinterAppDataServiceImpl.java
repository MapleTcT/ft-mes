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

import java.util.Collections;
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
        return queryPageData(pageDataQueryBO, ServiceTypeEnum.APP.getCode(), null);
    }

    @Override
    public List<PageDataBO> getPageList(PageDataQueryBO pageDataQueryBO) {
        return queryPageData(pageDataQueryBO, ServiceTypeEnum.PAGE.getCode(), pageDataQueryBO.getPCode());
    }

    private List<PageDataBO> queryPageData(PageDataQueryBO pageDataQueryBO, Integer serviceType, String entityCode) {
        PrinterRegisterBO queryBO = new PrinterRegisterBO();
        queryBO.setSource(pageDataQueryBO.getSource());
        queryBO.setServiceType(serviceType);

        PrinterRegisterBO registerBO = printerRegisterService.queryPrinterRegister(queryBO);
        if (registerBO == null || StringUtils.isBlank(registerBO.getServiceUrl())) {
            log.warn("printer register not found, source={}, serviceType={}", pageDataQueryBO.getSource(), serviceType);
            return Collections.emptyList();
        }

        String url = nacosInstance.selectOneHealthyInstance(registerBO.getSource(), registerBO.getServiceUrl());
        if (StringUtils.isBlank(url)) {
            log.warn("printer service url is blank, source={}, serviceType={}", registerBO.getSource(), serviceType);
            return Collections.emptyList();
        }

        if (StringUtils.isNotBlank(entityCode)) {
            url = url + "?entityCode=" + entityCode;
        }

        try {
            AppDataResponseBO responseBO = restTemplate.getForObject(url, AppDataResponseBO.class);
            if (responseBO == null || responseBO.getData() == null || responseBO.getData().getData() == null) {
                log.warn("printer app data response is empty, source={}, serviceType={}", registerBO.getSource(), serviceType);
                return Collections.emptyList();
            }
            return responseBO.getData().getData();
        } catch (RuntimeException ex) {
            log.warn("printer app data request failed, source={}, serviceType={}, url={}", registerBO.getSource(), serviceType, url, ex);
            return Collections.emptyList();
        }
    }
}
