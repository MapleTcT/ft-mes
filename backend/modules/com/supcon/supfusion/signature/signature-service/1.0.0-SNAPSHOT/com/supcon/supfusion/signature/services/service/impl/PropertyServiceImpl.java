package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.signature.dao.entity.Property;
import com.supcon.supfusion.signature.dao.mappers.PropertyMapper;
import com.supcon.supfusion.signature.services.service.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class PropertyServiceImpl extends ServiceImpl<PropertyMapper, Property> implements PropertyService {
    @Autowired
    private PropertyMapper propertyMapper;

    @Override
    public Set<Property> findPropertiesByModelCode(String modelCode) {
        List<Property> properties = propertyMapper.selectList(new LambdaQueryWrapper<Property>().eq(Property::getModelCode, modelCode));
        return new HashSet<Property>(properties);
    }
}
