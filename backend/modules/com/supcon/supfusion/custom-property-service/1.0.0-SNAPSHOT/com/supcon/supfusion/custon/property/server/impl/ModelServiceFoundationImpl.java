package com.supcon.supfusion.custon.property.server.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.common.enums.CoustomPropertyErrorEnum;
import com.supcon.supfusion.custon.property.common.exception.CoustomPropertyException;
import com.supcon.supfusion.custon.property.dao.entity.*;
import com.supcon.supfusion.custon.property.dao.mappers.*;
import com.supcon.supfusion.custon.property.server.*;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyModelBO;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyViewBO;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author zhang yafei
 */
@Service
@Slf4j
public class ModelServiceFoundationImpl extends ServiceImpl<ModelMapeer, Model> implements ModelServiceFoundation {
    @Autowired
    private ModelMapeer modelMapeer;

    @Autowired
    CustomPropertyModelMappingService customPropertyModelMappingService;

    @Autowired
    private PropertyMapper propertyMapper;

    @Autowired
    private PropertyProjectMapper propertyProjectMapper;

    @Autowired
    PropertyService propertyService;

    @Autowired
    PropertyProjectService propertyProjectService;

    @Autowired
    private CustomPropertyModelMappingMapper customPropertyModelMappingMapper;

    @Autowired
    private CustomPropertyViewMappingMapper customPropertyViewMappingDao;
    @Autowired
    private CustomPropertyViewMappingService customPropertyViewMappingService;

    @Override
    @Transactional
    public List<Model> getModels(String entityCode) {

        return modelMapeer.selectList(new LambdaQueryWrapper<Model>().eq(Model::getEntityCode, entityCode));
    }

    @Override
    @Transactional
    public List<CustomPropertyModelBO> findCustomPropertyModelMappings(String modelCode) {

        List<CustomPropertyModelBO> rs = new ArrayList<>();
        LambdaQueryWrapper<Property> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Property::getModelCode, modelCode)
                .eq(Property::getIsCustom, true)
                .orderByAsc(Property::getName);
        List<Property> properties = propertyMapper.selectList(lambdaQueryWrapper);
        if (properties != null && properties.size() > 0) {
            LambdaQueryWrapper<CustomPropertyModel> modelMappingLambdaQueryWrapper = new LambdaQueryWrapper<>();
            modelMappingLambdaQueryWrapper.eq(CustomPropertyModel::getModelCode, modelCode)
                    .orderByDesc(CustomPropertyModel::getSort, CustomPropertyModel::getEnableCustom)
                    .orderByAsc(CustomPropertyModel::getPropertyCode);
            List<CustomPropertyModel> list = customPropertyModelMappingMapper.selectList(modelMappingLambdaQueryWrapper);
            list.forEach(customPropertyModelMapping -> {
                CustomPropertyModelBO customPropertyModelMappingBO = new CustomPropertyModelBO();
                BeanUtils.copyProperties(customPropertyModelMapping, customPropertyModelMappingBO);
                customPropertyModelMappingBO.setProperty(propertyMapper.selectById(customPropertyModelMapping.getPropertyCode()));
                rs.add(customPropertyModelMappingBO);
            });


            for (Property p : properties) {
                Boolean flag = false;
                for (CustomPropertyModel m : list) {
                    if (p.getCode().equals(m.getPropertyCode())) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    CustomPropertyModelBO modelMapping = generateCustomPropertyModelMapping(p, false);
                    rs.add(modelMapping);
                }
            }
        }
        rs.forEach(customPropertyModelMappingBO -> {
            String associatedPropertyCode = customPropertyModelMappingBO.getAssociatedPropertyCode();
            if (StringUtils.isNotBlank(associatedPropertyCode)) {
                Property property = propertyMapper.selectById(associatedPropertyCode);
                customPropertyModelMappingBO.setAssociatedProperty(property);
            }
        });
        //排序
        Collections.sort(rs, new Comparator<CustomPropertyModelBO>() {
            @Override
            public int compare(CustomPropertyModelBO o1, CustomPropertyModelBO o2) {
                if (o1.getSort() != null && o2.getSort() != null && Boolean.TRUE.equals(o1.getEnableCustom()) && Boolean.TRUE.equals(o2.getEnableCustom())) {
                    return o1.getSort() - o2.getSort();
                } else if (o1.getSort() != null && o2.getSort() == null && Boolean.TRUE.equals(o1.getEnableCustom()) && Boolean.TRUE.equals(o2.getEnableCustom())) {
                    return -1;
                } else if (o1.getSort() == null && o2.getSort() != null && Boolean.TRUE.equals(o1.getEnableCustom()) && Boolean.TRUE.equals(o2.getEnableCustom())) {
                    return 1;
                } else if (Boolean.TRUE.equals(o1.getEnableCustom()) && Boolean.FALSE.equals(o2.getEnableCustom())) {
                    return -1;
                } else if (Boolean.FALSE.equals(o1.getEnableCustom()) && Boolean.FALSE.equals(o2.getEnableCustom())) {
                    return 1;
                } else if (o1.getId() != null && o2.getId() != null && Boolean.TRUE.equals(o1.getEnableCustom()) && Boolean.TRUE.equals(o2.getEnableCustom())) {
                    return (o1.getId() - o2.getId()) > 0 ? 1 : -1;
                } else {
                    return 0;
                }
            }
        });
        return rs;
    }


    @Override
    @Transactional
    public void saveCustomPropertyModelMapping(CustomPropertyModelBO modelMappingBO) {

        Property property = propertyMapper.selectById(modelMappingBO.getPropertyCode());
        PropertyProject propertyProject = propertyProjectMapper.selectById(modelMappingBO.getPropertyCode());
        if (StringUtils.isNotBlank(modelMappingBO.getAssociatedPropertyCode())) {
            Property associated = getProperty(modelMappingBO.getAssociatedPropertyCode());
            if (associated != null) {
                Model assoModel = getModel(associated.getModelCode());
                Model model = getModel(property.getModelCode());
                if (!model.getCode().equals(assoModel.getCode()) && assoModel.getModelName().equalsIgnoreCase(model.getModelName())) {
                    throw new CoustomPropertyException(CoustomPropertyErrorEnum.BE_EMPTY_ERROR);
                }
            }
        }

        CustomPropertyModel m = customPropertyModelMappingMapper.selectOne(new LambdaQueryWrapper<CustomPropertyModel>().eq(CustomPropertyModel::getPropertyCode, modelMappingBO.getProperty().getCode()));

        if (m != null) {
            m.setDisplayName(modelMappingBO.getDisplayName());
            m.setFieldType(modelMappingBO.getFieldType());
            m.setFormat(modelMappingBO.getFormat());
            m.setFillContent(modelMappingBO.getFillContent());
            m.setMultable(modelMappingBO.getMultable());
            m.setSeniorSystemCode(modelMappingBO.getSeniorSystemCode());
            m.setAssociatedPropertyCode(modelMappingBO.getAssociatedPropertyCode());
            m.setAssociatedType(modelMappingBO.getAssociatedType());
            m.setNullable(modelMappingBO.getNullable());
            m.setDescription(modelMappingBO.getDescription());
            m.setEnableCustom(modelMappingBO.getEnableCustom());
            m.setReferenceViewCode(modelMappingBO.getReferenceViewCode());
            m.setRelatedKey(modelMappingBO.getRelatedKey());
            m.setSort(modelMappingBO.getSort());
            m.setPrecision(modelMappingBO.getPrecision());
        } else {
            m = new CustomPropertyModel();
            BeanUtils.copyProperties(modelMappingBO, m);
            m.setPropertyCode(modelMappingBO.getProperty().getCode());
            m.setId(IDGenerator.newInstance().generate().longValue());
        }
        m.setModelCode(property.getModelCode());
        customPropertyModelMappingService.saveOrUpdate(m);

        if (property != null) {
            property.setDisplayName(m.getDisplayName());
            property.setFieldType(m.getFieldType());
            property.setFormat(m.getFormat());
            property.setFillcontent(m.getFillContent());
            property.setMultable(m.getMultable());
            property.setSeniorSystemCode(m.getSeniorSystemCode());
//            p.setAssociatedPropertyCode(m.getAssociatedPropertyCode());
            property.setAssociatedType(m.getAssociatedType());
            property.setNullable(m.getNullable());
            property.setDescription(m.getDescription());
            property.setProjCustomInUse(m.getEnableCustom());
            property.setIsUsedForList(m.getEnableCustom());
            property.setIsUsedForSearch(m.getEnableCustom());
            property.setProjFlag(m.getEnableCustom());
            property.setFillcontent(m.getFillContent());
            propertyService.saveOrUpdate(property);

            propertyProject.setDisplayName(m.getDisplayName());
            propertyProject.setFieldType(m.getFieldType());
            propertyProject.setFormat(m.getFormat());
            propertyProject.setFillcontent(m.getFillContent());
            propertyProject.setMultable(m.getMultable());
            propertyProject.setSeniorSystemCode(m.getSeniorSystemCode());
//            p.setAssociatedPropertyCode(m.getAssociatedPropertyCode());
            propertyProject.setAssociatedType(m.getAssociatedType());
            propertyProject.setNullable(m.getNullable());
            propertyProject.setDescription(m.getDescription());
            propertyProject.setProjCustomInUse(m.getEnableCustom());
            propertyProject.setIsUsedForList(m.getEnableCustom());
            propertyProject.setIsUsedForSearch(m.getEnableCustom());
            propertyProject.setProjFlag(m.getEnableCustom());
            propertyProject.setFillcontent(m.getFillContent());
            propertyProjectService.saveOrUpdate(propertyProject);

        }
        //如果模型内的字段启用时的状态为不可空，则需要向viewmapping内写入数据
        updateViewData(m, property);
    }

    private void updateViewData(CustomPropertyModel modelMapping, Property property) {
        LambdaQueryWrapper<CustomPropertyView> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CustomPropertyView::getPropertyCode, modelMapping.getPropertyCode())
                .eq(CustomPropertyView::getAssociatedCode, property.getEntityCode() + "_edit");
        CustomPropertyView viewMapping = customPropertyViewMappingDao.selectOne(lambdaQueryWrapper);
        //如果模型内的字段启用时的状态为不可空，则需要向viewmapping内写入数据
        if (Boolean.FALSE.equals(modelMapping.getNullable()) && Boolean.TRUE.equals(modelMapping.getEnableCustom())) {
            if (viewMapping == null) {
                viewMapping = generateCustomPropertyViewMapping(property, modelMapping.getEnableCustom());
                customPropertyViewMappingService.saveOrUpdate(viewMapping);
            }
        }

        if ("sysbase_1.0".equals(property.getModuleCode())) {
            if (viewMapping == null) {
                viewMapping = generateCustomPropertyViewMapping(property, modelMapping.getEnableCustom());
            }
            viewMapping.setShowCustom(modelMapping.getEnableCustom());
            viewMapping.setNullable(modelMapping.getNullable());
            customPropertyViewMappingService.saveOrUpdate(viewMapping);
        }
    }

    @Override
    public Property getProperty(String code) {
        return propertyMapper.selectById(code);

    }

    @Override
    public CustomPropertyModelBO generateCustomPropertyModelMapping(Property p, Boolean enabled) {

        CustomPropertyModelBO m = new CustomPropertyModelBO();
        m.setDisplayName(p.getDisplayName());
        m.setFieldType(p.getFieldType());
        m.setFormat(p.getFormat());
        m.setFillContent(p.getFillcontent());
        m.setMultable(p.getMultable());
        m.setSeniorSystemCode(p.getSeniorSystemCode());
        m.setAssociatedPropertyCode(p.getAssociatedPropertyCode());
        m.setAssociatedType(p.getAssociatedType());
        m.setNullable(p.getNullable());
        m.setDescription(p.getDescription());
        m.setEnableCustom(enabled);
        m.setProperty(p);
        m.setPropertyCode(p.getCode());
        m.setModelCode(p.getModelCode());
        return m;
    }

    @Override
    public Model getModel(String code) {
        Model model = modelMapeer.selectById(code);
        return model;
    }

    @Override
    public List<Model> getModelBycode(String code) {
        LambdaQueryWrapper<Model> modelLambdaQueryWrapper = new LambdaQueryWrapper<>();
        modelLambdaQueryWrapper.like(Model::getCode, "%" + code);
        return modelMapeer.selectList(modelLambdaQueryWrapper);

    }

    @Override
    @Transactional
    public void enableProperty(List<String> codes, List<Long> ids, Boolean enabled) {
        List<String> propertyCode = new ArrayList<>();
        if (ids != null && ids.size() > 0) {
            List<CustomPropertyModel> customPropertyModels = customPropertyModelMappingMapper.selectBatchIds(ids);
            for (CustomPropertyModel modelMapping : customPropertyModels) {
                if (modelMapping != null) {
                    modelMapping.setEnableCustom(enabled);
                    if (Boolean.FALSE.equals(enabled)) {
                        modelMapping.setSort(null);
                    }
                    propertyCode.add(modelMapping.getPropertyCode());
                    Property property = propertyMapper.selectById(modelMapping.getPropertyCode());
                    PropertyProject propertyProject = propertyProjectMapper.selectById(modelMapping.getPropertyCode());
                    if (property != null) {
                        property.setProjCustomInUse(enabled);
                        property.setIsUsedForList(enabled);
                        property.setIsUsedForSearch(enabled);
                        property.setProjFlag(enabled);
                        propertyService.saveOrUpdate(property);

                        propertyProject.setProjCustomInUse(enabled);
                        propertyProject.setIsUsedForList(enabled);
                        propertyProject.setIsUsedForSearch(enabled);
                        propertyProject.setProjFlag(enabled);
                        propertyProjectService.saveOrUpdate(propertyProject);
                    }

                    updateViewData(modelMapping, property);

                }
            }
            customPropertyModelMappingService.saveOrUpdateBatch(customPropertyModels);
        }
        if (codes != null && codes.size() > 0) {
            List<Property> properties = propertyMapper.selectBatchIds(codes);
            List<PropertyProject> propertyProjects = propertyProjectMapper.selectBatchIds(codes);
            for (Property prop : properties) {
                LambdaQueryWrapper<CustomPropertyModel> eq = new LambdaQueryWrapper<CustomPropertyModel>()
                        .eq(CustomPropertyModel::getPropertyCode, prop.getCode());
                CustomPropertyModel modelMapping = customPropertyModelMappingMapper.selectOne(eq);
                if (modelMapping == null) {
                    CustomPropertyModelBO customPropertyModelMappingBO = generateCustomPropertyModelMapping(prop, enabled);
                    modelMapping = new CustomPropertyModel();
                    BeanUtils.copyProperties(customPropertyModelMappingBO, modelMapping);
                    modelMapping.setId(IDGenerator.newInstance().generate().longValue());
                } else {
                    modelMapping.setEnableCustom(enabled);
                }
                if (Boolean.FALSE.equals(enabled)) {
                    modelMapping.setSort(null);
                }
                if (prop != null) {
                    prop.setProjCustomInUse(enabled);
                    prop.setIsUsedForList(enabled);
                    prop.setIsUsedForSearch(enabled);
                    prop.setProjFlag(enabled);
                    propertyProjects.forEach(propertyProject -> {
                        if (prop.getCode().equals(propertyProject.getCode())){
                            propertyProject.setProjCustomInUse(enabled);
                            propertyProject.setIsUsedForList(enabled);
                            propertyProject.setIsUsedForSearch(enabled);
                            propertyProject.setProjFlag(enabled);
                            return;
                        }
                    });
                }
                customPropertyModelMappingService.saveOrUpdate(modelMapping);
                updateViewData(modelMapping, prop);

            }
            propertyService.saveOrUpdateBatch(properties);
            propertyProjectService.saveOrUpdateBatch(propertyProjects);
        }
    }

    @Override
    public Property getPKProperty(String modelCode) {
        LambdaQueryWrapper<Property> propertyLambdaQueryWrapper = new LambdaQueryWrapper<>();
        propertyLambdaQueryWrapper.eq(Property::getModelCode, modelCode)
                .eq(Property::getIsPk, true);
        Property propertie = propertyMapper.selectOne(propertyLambdaQueryWrapper);
        if (propertie != null) {
            return propertie;
        }

        return propertyMapper.selectOne(new LambdaQueryWrapper<Property>().eq(Property::getModelCode, modelCode).eq(Property::getColumnName, "id"));
    }

    @Override
    public void modelManageSort(List<Long> ids) {
        List<CustomPropertyModel> customPropertyModels = customPropertyModelMappingMapper.selectBatchIds(ids);
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            for (int y = 0; y < customPropertyModels.size(); y++) {
                CustomPropertyModel customPropertyModel = customPropertyModels.get(y);
                if (id.equals(customPropertyModel.getId())) {
                    customPropertyModel.setSort(i + 1);
                    break;
                }
            }

        }
        customPropertyModelMappingService.updateBatchById(customPropertyModels);
    }


    public CustomPropertyView generateCustomPropertyViewMapping(Property p, Boolean enabled) {

        CustomPropertyView v = new CustomPropertyView();
        v.setId(IDGenerator.newInstance().generate().longValue());
        v.setDisplayName(p.getDisplayName());
        v.setFieldType(p.getFieldType());
        v.setFormat(p.getFormat());
//        v.setMultable(p.getMultable());
        v.setNullable(Boolean.FALSE);
        v.setShowCustom(enabled);
//        v.setProperty(p);
        v.setAssociatedCode(p.getEntityCode() + "_edit");
        v.setPropertyLayRec(null);
        return v;
    }

}
