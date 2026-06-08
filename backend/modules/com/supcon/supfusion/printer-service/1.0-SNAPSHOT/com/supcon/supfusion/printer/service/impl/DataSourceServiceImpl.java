package com.supcon.supfusion.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.printer.common.JSONObject;
import com.supcon.supfusion.printer.common.RpcUtil;
import com.supcon.supfusion.printer.config.NacosInstance;
import com.supcon.supfusion.printer.dao.mapper.EntityPageUrlMapper;
import com.supcon.supfusion.printer.dao.mapper.PrinterRegisterMapper;
import com.supcon.supfusion.printer.dao.po.EntityPageUrlPO;
import com.supcon.supfusion.printer.dao.po.PrinterRegisterPO;
import com.supcon.supfusion.printer.service.DataSourceService;
import com.supcon.supfusion.printer.service.bo.*;
import com.supcon.supfusion.printer.service.enums.ServiceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DataSourceServiceImpl implements DataSourceService {

    @Autowired
    private EntityPageUrlMapper entityPageUrlMapper;

    @Autowired
    private PrinterRegisterMapper printerRegisterMapper;

    @Autowired
    @Qualifier("restTemplateClient")
    private RestTemplate restTemplate;

    @Autowired
    private RestTemplate restTemplateClient;

    @Autowired
    private NacosInstance nacosInstance;

    /**
     * 自定义服务调用
     * 返回数据格式
     * @param url 自定义服务url
     * @return
     */
    @Override
    public Object callCustomService(String url, Integer process) {
        String sign = "?";
        if (StringUtils.indexOf(url, "?") != -1) {
            sign = "&";
        }
        try {
            JSONObject jb = restTemplateClient.getForObject(String.format(url + "%sprocess=" + process, sign), JSONObject.class);
            if (RpcUtil.isSuccess(jb)) {
                return jb.get("data");
            }
        } catch(Exception e) {
            log.error("自定义服务请求异常, 请求地址: {}, 异常信息: {}", url, e);
            return null;
        }
        return null;
    }

    /**
     * 实体iframe url列表
     * @return
     */
    @Override
    public List<EntityPageUrlBO> listEntites() {
        QueryWrapper<EntityPageUrlPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(EntityPageUrlPO::getValid, true);
        List<EntityPageUrlPO> pos = entityPageUrlMapper.selectList(queryWrapper);
        if (pos == null || pos.size() == 0) {
            return null;
        }
        List<EntityPageUrlBO> bos = new ArrayList<>(pos.size());
        pos.stream().forEach(item -> {
            EntityPageUrlBO entityPageUrlBO = new EntityPageUrlBO();
            BeanUtils.copyProperties(item, entityPageUrlBO);
            bos.add(entityPageUrlBO);
        });
        return bos;
    }

    /**
     * 根据表单ID查询实体数据
     * @param entityQueryConditionBO 实体数据结果
     * @return
     */
    @Override
    public List<EntityDataResultBO> getEntityData(EntityQueryConditionBO entityQueryConditionBO) {
        QueryWrapper<PrinterRegisterPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrinterRegisterPO::getServiceType, ServiceTypeEnum.DATA.getCode());
        PrinterRegisterPO printerRegisterPO = printerRegisterMapper.selectOne(queryWrapper);
        if (printerRegisterPO == null) {
            return null;
        }
        log.info("getEntityData 参数:{}", entityQueryConditionBO);
        String url = nacosInstance.selectOneHealthyInstance(printerRegisterPO.getSource(), printerRegisterPO.getServiceUrl());
        if (StringUtils.isBlank(url)) {
            return null;
        }
        JSONObject json = restTemplate.postForObject(url, entityQueryConditionBO, JSONObject.class);
        log.info("getEntityData 响应:{}", json);
        EntityDataRestResultBO responseEntity = restTemplate.postForObject(url, entityQueryConditionBO, EntityDataRestResultBO.class);
        if (responseEntity == null || responseEntity.getData() == null) {
            return null;
        }
        return responseEntity.getData().getData();
    }

    /**
     * 根据实体编码（app）
     * @param entityCode
     * @return
     */
    @Override
    public List<ModelBO> getModelsByEntityCode(String entityCode) {
        QueryWrapper<PrinterRegisterPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrinterRegisterPO::getServiceType, ServiceTypeEnum.MODEL.getCode());
        PrinterRegisterPO printerRegisterPO = printerRegisterMapper.selectOne(queryWrapper);
        if (printerRegisterPO == null) {
            return null;
        }
        String url = nacosInstance.selectOneHealthyInstance(printerRegisterPO.getSource(), printerRegisterPO.getServiceUrl());
        if (StringUtils.isBlank(url)) {
            return null;
        }
        JSONObject json = restTemplate.getForObject(url + "?entityCode=" + entityCode, JSONObject.class);
        log.info("getModelsByEntityCode 请求url：{}，结果{}", url + "?entityCode=" + entityCode, json);
        ModelRestResultBO responseEntity = restTemplate.getForObject(url + "?entityCode=" + entityCode, ModelRestResultBO.class);
        if (responseEntity == null || responseEntity.getData() == null) {
            return null;
        }
        return responseEntity.getData().getData();
    }

    @Override
    public List<EntityModelBO> getSubProperties(String modelCode, String propertyCode) {
        QueryWrapper<PrinterRegisterPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PrinterRegisterPO::getServiceType, ServiceTypeEnum.PROPERTY.getCode());
        PrinterRegisterPO printerRegisterPO = printerRegisterMapper.selectOne(queryWrapper);
        if (printerRegisterPO == null) {
            return null;
        }
        String url = nacosInstance.selectOneHealthyInstance(printerRegisterPO.getSource(), printerRegisterPO.getServiceUrl());
        if (StringUtils.isBlank(url)) {
            return null;
        }
        ModelConditionBO modelConditionBO = new ModelConditionBO();
        modelConditionBO.setModelCode(modelCode);
        modelConditionBO.setPropertyCode(propertyCode);
        JSONObject json = restTemplate.postForObject(url, modelConditionBO, JSONObject.class);
        log.info("getSubProperties 请求url：{}，结果{}", url + "?modelCode=" + modelCode + "&propertyCode=" + propertyCode, json);
        EntityModelRestResultBO responseEntity = restTemplate.postForObject(url, modelConditionBO, EntityModelRestResultBO.class);
        if (responseEntity == null || responseEntity.getData() == null) {
            return null;
        }
        return responseEntity.getData().getData();
    }
}
