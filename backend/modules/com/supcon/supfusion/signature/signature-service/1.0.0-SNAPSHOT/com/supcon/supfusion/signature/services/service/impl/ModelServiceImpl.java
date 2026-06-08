package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.signature.dao.dto.ModelTableDto;
import com.supcon.supfusion.signature.dao.entity.Model;
import com.supcon.supfusion.signature.dao.entity.Property;
import com.supcon.supfusion.signature.dao.mappers.ModelMapeer;
import com.supcon.supfusion.signature.dao.utils.Inflector;
import com.supcon.supfusion.signature.services.service.ModelService;
import com.supcon.supfusion.signature.services.service.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class ModelServiceImpl extends ServiceImpl<ModelMapeer, Model> implements ModelService {
    @Autowired
    private ModelMapeer modelMapper;

    @Autowired
    private PropertyService propertyService;


    @Override
    public Model getModel(String modelCode) {
        return modelMapper.selectById(modelCode);
    }

    @Override
    public String getPropertyColumnName(String modelCode, String propertyName, Boolean isObjectType) {
        if (modelCode != null && modelCode.length() > 0) {
            Model model = getModel(modelCode);
            if (model != null) {
                Set<Property> props = propertyService.findPropertiesByModelCode(model.getCode());
                for (Property p : props) {
                    if (propertyName.equals(p.getName())) {
                        return p.getColumnName();
                    }
                }
            }
        }
        if (isObjectType != null && isObjectType) {
            return Inflector.getInstance().columnize(propertyName) + "_ID";
        }
        return Inflector.getInstance().columnize(propertyName);
    }

    @Override
    public List<ModelTableDto> getModelTableByTableInfoId(String dealInfoTableName, String columnName, Long tableInfoId) {
        return modelMapper.getModelTableByTableInfoId(dealInfoTableName, columnName, tableInfoId);
    }

    @Override
    public List<ModelTableDto> getModelTableByDealInfoId(String dealInfoTable, Long tableInfoId) {
        return modelMapper.getModelTableByDealInfoId(dealInfoTable, tableInfoId);
    }

}
