package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.services.CustomPropertyService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;

import java.util.List;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@ServiceApiService
public class CustomPropertyServiceImpl implements CustomPropertyService {
    @Override
    public List findCPByBusinessValue(String modelCode, String businessModel, Object... businessValue) {
        return null;
    }
}
