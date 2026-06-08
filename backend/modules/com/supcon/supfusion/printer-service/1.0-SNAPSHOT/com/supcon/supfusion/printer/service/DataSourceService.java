package com.supcon.supfusion.printer.service;

import com.supcon.supfusion.printer.service.bo.*;

import java.util.List;

public interface DataSourceService {

    /**
     * 调用自定义服务
     * @param url 自定义服务url
     * @return
     */
    Object callCustomService(String url, Integer process);

    /**
     * 实体iframe url列表
     * @return
     */
    List<EntityPageUrlBO> listEntites();

    List<EntityDataResultBO> getEntityData(EntityQueryConditionBO entityQueryConditionBO);

    /**
     * 根据实体编码（app）
     * @param entityCode
     * @return
     */
    List<ModelBO> getModelsByEntityCode(String entityCode);

    /**
     * 模型列表动态加载子属性
     * @param modelCode
     * @param propertyCode
     * @return
     */
    List<EntityModelBO> getSubProperties(String modelCode, String propertyCode);
}
