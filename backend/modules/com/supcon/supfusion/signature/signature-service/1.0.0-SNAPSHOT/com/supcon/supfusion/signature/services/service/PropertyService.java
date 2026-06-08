package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.signature.dao.entity.Property;

import java.util.Set;

/**
 * @author zhang yafei
 */
public interface PropertyService extends IService<Property> {
    Set<Property> findPropertiesByModelCode(String moduleCode);
}
