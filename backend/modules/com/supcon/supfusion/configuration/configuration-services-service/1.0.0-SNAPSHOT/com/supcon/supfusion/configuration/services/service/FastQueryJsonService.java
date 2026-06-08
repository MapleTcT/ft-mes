package com.supcon.supfusion.configuration.services.service;

import com.supcon.supfusion.configuration.services.entity.FastQueryJson;
import org.hibernate.criterion.Criterion;

import java.util.List;

/**
 * 快速查询.
 *
 * @author fukun
 *
 */
public interface FastQueryJsonService {

       /**
        * 根据条件查询FastQueryJson
        *
        * @param criterions
        * @return
        */
       List<FastQueryJson> findFastQueryJsons(Criterion... criterions);

       /**
        * 新布局里面快速查询保存字段
        *
        * @param fastQueryJson
        * @param fieldConfig
        */
       void saveFields(FastQueryJson fastQueryJson, String fieldConfig);

       /**
        *      根据code查询快速查询对象
        *
        * @param code
        * @return
        */
       FastQueryJson getFastQueryJson(String code);

       /**
        *  物理删除fqj
        *
        * @param fqj
        */
       void deletePhysical(FastQueryJson fqj);

       /**
        * 删除Field
        *
        */
       void deleteField(FastQueryJson fqj,boolean flag);
}
