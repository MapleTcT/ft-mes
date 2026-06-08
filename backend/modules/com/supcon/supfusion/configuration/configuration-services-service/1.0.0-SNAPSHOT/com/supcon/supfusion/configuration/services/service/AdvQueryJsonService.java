package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.AdvQueryJson;
import org.hibernate.criterion.Criterion;

import java.util.List;

/**
 * 高级查询.
 *
 * @author fukun
 *
 */
public interface AdvQueryJsonService {

       /**
        * 根据条件查询AdvQueryJson
        *
        * @param criterions
        * @return
        */
       List<AdvQueryJson> findAdvQueryJsons(Criterion... criterions);

       /**
        * 新布局里面高级查询保存字段
        *
        * @param advQueryJson
        * @param fieldConfig
        */
       void saveFields(AdvQueryJson advQueryJson, String fieldConfig);

       /**
        *      根据code查询高级查询对象
        *
        * @param code
        * @return
        */
       AdvQueryJson getAdvQueryJson(String code);

       /**
        *  物理删除aqj
        *
        * @param aqj
        */
       void deletePhysical(AdvQueryJson aqj);

       /**
        * 删除Field
        *
        */
       void deleteField(AdvQueryJson aqj,boolean flag);
}