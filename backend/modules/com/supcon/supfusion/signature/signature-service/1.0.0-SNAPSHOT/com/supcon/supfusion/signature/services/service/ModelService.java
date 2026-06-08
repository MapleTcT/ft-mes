package com.supcon.supfusion.signature.services.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.signature.dao.dto.ModelTableDto;
import com.supcon.supfusion.signature.dao.entity.Model;
import com.supcon.supfusion.signature.dao.mappers.ModelMapeer;

import java.util.List;

/**
 * @author zhang yafei
 */
public interface ModelService  extends IService<Model> {

    Model getModel(String modelCode);

    String getPropertyColumnName(String modelCode, String propertyName, Boolean isObjectType);

    List<ModelTableDto> getModelTableByTableInfoId(String dealInfoTableName, String columnName, Long tableInfoId);

    List<ModelTableDto> getModelTableByDealInfoId(String dealInfoTable, Long tableInfoId);
}
