package com.supcon.supfusion.custon.property.server.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.common.i18n.InternationalResource;
import com.supcon.supfusion.custon.property.dao.entity.*;
import com.supcon.supfusion.custon.property.dao.mappers.*;
import com.supcon.supfusion.custon.property.dao.utils.SerializeUitls;
import com.supcon.supfusion.custon.property.server.*;
import com.supcon.supfusion.custon.property.server.bo.CustomPropertyViewBO;
import com.supcon.supfusion.custon.property.server.bo.ViewEnabledStatusCodeBO;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class ViewServiceFoundationImpl extends ServiceImpl<ViewMapper, View> implements ViewServiceFoundation {

    @Autowired
    private ViewMapper viewMapper;

    @Autowired
    private ExtraViewMapper extraViewMapper;

    @Autowired
    private DataGridService dataGridService;

    @Autowired
    private DataGridMapper dataGridMapper;

    @Autowired
    private InternationalResource internationalResource;

    @Autowired
    private CustomPropertyModelMappingMapper customPropertyModelMappingMapper;

    @Autowired
    private CustomPropertyViewMappingMapper customPropertyViewMappingMapper;

    @Autowired
    private CustomPropertyViewMappingService customPropertyViewMappingService;

    @Autowired
    private ModelMapeer modelMapeer;

    @Autowired
    private ModelServiceFoundation modelServiceFoundation;

    @Autowired
    private EcConfigService ecConfigService;

    @Autowired
    private PropertyMapper propertyMapper;


    @Override
    public List<View> findViews(String entityCode, ViewType... viewTypes) {
        LambdaQueryWrapper<View> viewLambdaQueryWrapper = new LambdaQueryWrapper<>();
        viewLambdaQueryWrapper.eq(View::getEntityCode, entityCode).in(View::getType, viewTypes);
        return viewMapper.selectList(viewLambdaQueryWrapper);
    }

    @Override
    public List<View> findViewsByModelCode(String modelCode, ViewType... viewTypes) {
        LambdaQueryWrapper<View> viewLambdaQueryWrapper = new LambdaQueryWrapper<>();
        viewLambdaQueryWrapper.eq(View::getAssModelCode, modelCode).in(View::getType, viewTypes);
        return viewMapper.selectList(viewLambdaQueryWrapper);
    }

    @Override
    public void viewManageSort(List<Long> ids) {
        List<CustomPropertyView> customPropertyViews = customPropertyViewMappingMapper.selectBatchIds(ids);
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            for (int y = 0; y < customPropertyViews.size(); y++) {
                CustomPropertyView customPropertyView = customPropertyViews.get(y);
                if (id .equals(customPropertyView.getId())){
                    customPropertyView.setSort(i + 1);
                    break;
                }
            }

        }
        customPropertyViewMappingService.updateBatchById(customPropertyViews);
    }

    @Override
    public View getView(String viewCode) {
        return viewMapper.selectById(viewCode);
    }

    @Override
    @Transactional
    public List<CustomPropertyViewBO> findCustomPropertyViewMappings(View view, Boolean isDataGrid) {

        Model assModel = modelMapeer.selectById(view.getAssModelCode());
        List<CustomPropertyViewBO> viewTree = new ArrayList<>();
        ExtraView extraView = extraViewMapper.selectOne(new LambdaQueryWrapper<ExtraView>().eq(ExtraView::getViewCode, view.getCode()));
        if (view.getIsShadow()) {

            extraView = extraViewMapper.selectOne(new LambdaQueryWrapper<ExtraView>().eq(ExtraView::getCode, view.getShadowViewCode()));
        }
        if (extraView == null) {
            return viewTree;
        }
        if (ViewType.EDIT.equals(view.getType()) || ViewType.VIEW.equals(view.getType()) || ViewType.EXTRA.equals(view.getType())) {
            String config = extraView.getConfig();
            String viewJson = extraView.getViewJson();
            if (config != null && config.contains("<customSection><![CDATA[true]]></customSection>")) { // 包含自定义节
                CustomPropertyViewBO m = new CustomPropertyViewBO();
                m.setDisplayName(assModel.getName());
                m.setNullable(false);
                m.set_code(assModel.getCode());
                m.setLayRec(assModel.getCode());
                m.setIsParent(true);

                List<CustomPropertyViewBO> list = generateCustomPropertyViewMappings(assModel, view.getCode(), assModel.getCode(),
                        m.getLayRec(), true, null, view.getType());
                if (list != null && list.size() > 0) {
                    if (StringUtils.isNotBlank(viewJson)) {
                        JSONObject jsonObject = JSON.parseObject(viewJson);
                        Integer colNum = jsonObject.getInteger("colNum");
                        if (colNum != null) {
                            list.forEach(c->{
                                c.setTextareaRow(colNum);
                            });
                        }
                    }
                    m.setList(list);
                    viewTree.add(m);
                }
            }
            if (isDataGrid) {
                String dataGridViewCode = null;
                if (view.getIsShadow()) {
                    dataGridViewCode = view.getShadowViewCode();
                } else {
                    dataGridViewCode = view.getCode();
                }
                List<DataGrid> dgs = dataGridMapper.selectList(new LambdaQueryWrapper<DataGrid>().eq(DataGrid::getViewCode, dataGridViewCode));
                if (dgs != null && dgs.size() > 0) {
                    for (DataGrid dg : dgs) {
                        List<CustomPropertyViewBO> dgCustomePropertyByDgCode = findDgCustomePropertyByDgCode(dg, view.getType());
                        viewTree.addAll(dgCustomePropertyByDgCode);
                    }

                }
            }
        } else if (ViewType.LIST.equals(view.getType()) || ViewType.REFERENCE.equals(view.getType())) {
            String cfg = extraView.getConfig();
            if (cfg != null && cfg.contains("<customSection><![CDATA[true]]></customSection>")) {
                Map<String, Object> cfgMap = (Map<String, Object>) SerializeUitls.deserialize(cfg);
                if (cfgMap != null && cfgMap.size() > 0) {
                    Map<String, Object> layout = new HashMap<String, Object>();
                    if (view.getMobile()) {
                        layout = (Map<String, Object>) cfgMap.get("layout");
                        List<Map> tabList = (List<Map>) layout.get("tabs");
                        if (null != tabList && tabList.size() > 0) {
                            List<Map> layoutList = (List<Map>) (tabList.get(0).get("layout"));
                            if (null != layoutList && layoutList.size() > 1) {
                                layout = layoutList.get(1);
                            }
                        }
                    } else {
                        layout = (Map<String, Object>) cfgMap.get("layout");
                    }
                    if (layout != null && layout.size() > 0) {
                        List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
                        if (sections != null && sections.size() > 0) {
                            for (Map<String, Object> sec : sections) {
                                if ("LISTPT".equals(sec.get("regionType")) || view.getMobile()) {
                                    List<Map<String, Object>> cells = (List<Map<String, Object>>) sec.get("cells");
                                    if (cells != null && cells.size() > 0) {
                                        for (Map<String, Object> cell : cells) {
                                            if (cell.get("customSection") != null && cell.get("customSection").toString().equalsIgnoreCase("true")) {
                                                if (view.getMobile()) {
                                                    if (null != cell.get("element")) {
                                                        cell = (Map) cell.get("element");
                                                    }
                                                }
                                                String modelCode = (String) cell.get("customModelCode");
                                                String propLayRec = (String) cell.get("propertyLayRec");
                                                if (modelCode != null && modelCode.length() > 0) {
                                                    Model model = modelMapeer.selectById(modelCode);
                                                    CustomPropertyViewBO m = new CustomPropertyViewBO();
                                                    m.setDisplayName(model.getName());
                                                    m.setNullable(false);
                                                    m.setIsParent(true);
                                                    m.set_code(model.getCode());
                                                    m.setLayRec(modelCode + UUID.randomUUID().toString().replace("-", ""));
                                                    m.setPropertyLayRec(propLayRec);

                                                    List<CustomPropertyViewBO> list = generateCustomPropertyViewMappings(model, view.getCode(), m.get_code(),
                                                            m.getLayRec(), false, propLayRec, view.getType());
                                                    if (list != null && list.size() > 0) {
                                                        m.setList(list);
                                                        viewTree.add(m);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return viewTree;
    }


    private List<CustomPropertyViewBO> generateCustomPropertyViewMappings(Model model, String associatedCode, String parentCode, String layRec,
                                                                          boolean show, String propertyLayRec, ViewType viewType) {
        List<CustomPropertyViewBO> tree = new ArrayList<>();
        LambdaQueryWrapper<CustomPropertyModel> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(CustomPropertyModel::getModelCode, model.getCode())
                .eq(CustomPropertyModel::getEnableCustom, true)
                .orderByAsc(CustomPropertyModel::getId);
        List<CustomPropertyModel> modelMappingList = customPropertyModelMappingMapper.selectList(lambdaQueryWrapper);
        if (modelMappingList != null && modelMappingList.size() > 0) {
            LambdaQueryWrapper<CustomPropertyView> customPropertyViewMappingLambdaQueryWrapper = new LambdaQueryWrapper<>();
            customPropertyViewMappingLambdaQueryWrapper.eq(CustomPropertyView::getAssociatedCode, associatedCode);
            customPropertyViewMappingLambdaQueryWrapper.orderByDesc(CustomPropertyView::getShowCustom);
            if (propertyLayRec != null) {
                customPropertyViewMappingLambdaQueryWrapper.eq(CustomPropertyView::getPropertyLayRec, propertyLayRec);
            } else {
                customPropertyViewMappingLambdaQueryWrapper.isNull(CustomPropertyView::getPropertyLayRec);
            }
            List<CustomPropertyView> list = customPropertyViewMappingMapper.selectList(customPropertyViewMappingLambdaQueryWrapper);
            if (list != null && list.size() > 0) {
                for (Iterator<CustomPropertyView> iter1 = list.iterator(); iter1.hasNext(); ) {
                    CustomPropertyView viewMapping = iter1.next();
                    CustomPropertyViewBO customPropertyViewMappingBO = new CustomPropertyViewBO();
                    boolean flag = false;
                    for (Iterator<CustomPropertyModel> iter2 = modelMappingList.iterator(); iter2.hasNext(); ) {
                        CustomPropertyModel modelMapping = iter2.next();
                        if (modelMapping.getPropertyCode().equals(viewMapping.getPropertyCode())) {
                            viewMapping.setFieldType(modelMapping.getFieldType());
                            viewMapping.setFormat(modelMapping.getFormat());
                            customPropertyViewMappingBO.setRefViewCode(modelMapping.getReferenceViewCode());
                            flag = true;
                            iter2.remove();
                        }
                    }
                    if (!flag) {
                        iter1.remove();
                        continue;
                    }
                    BeanUtils.copyProperties(viewMapping, customPropertyViewMappingBO);
                    customPropertyViewMappingBO.set_code(viewMapping.getPropertyCode());
                    customPropertyViewMappingBO.set_parentCode(parentCode);
                    customPropertyViewMappingBO.setLayRec(layRec + "-" + customPropertyViewMappingBO.get_code());
                    Property property = propertyMapper.selectById(viewMapping.getPropertyCode());
                    customPropertyViewMappingBO.setProperty(property);
                    customPropertyViewMappingBO.setViewType(viewType);
                    tree.add(customPropertyViewMappingBO);
                }

            }
            for (CustomPropertyModel m : modelMappingList) {
                CustomPropertyViewBO customPropertyViewMappingBO = new CustomPropertyViewBO();
                BeanUtils.copyProperties(m, customPropertyViewMappingBO);
                customPropertyViewMappingBO.setId(null);
                customPropertyViewMappingBO.setShowCustom(false);
                customPropertyViewMappingBO.setPropertyLayRec(propertyLayRec);
                Property property = propertyMapper.selectById(m.getPropertyCode());
                customPropertyViewMappingBO.setProperty(property);
                customPropertyViewMappingBO.setAssociatedCode(associatedCode);
                customPropertyViewMappingBO.set_code(m.getPropertyCode());
                customPropertyViewMappingBO.set_parentCode(parentCode);
                customPropertyViewMappingBO.setViewType(viewType);
                customPropertyViewMappingBO.setRefViewCode(m.getReferenceViewCode());
                customPropertyViewMappingBO.setLayRec(layRec + "-" + customPropertyViewMappingBO.get_code());
                tree.add(customPropertyViewMappingBO);
            }
        }
        Collections.sort(tree, new Comparator<CustomPropertyViewBO>() {
            @Override
            public int compare(CustomPropertyViewBO o1, CustomPropertyViewBO o2) {
                if (o1.getSort() != null && o2.getSort() != null && Boolean.TRUE.equals(o1.getShowCustom()) && Boolean.TRUE.equals(o2.getShowCustom())) {
                    return o1.getSort() - o2.getSort();
                } else if (o1.getSort() != null && o2.getSort() == null && Boolean.TRUE.equals(o1.getShowCustom()) && Boolean.TRUE.equals(o2.getShowCustom())) {
                    return -1;
                } else if (o1.getSort() == null && o2.getSort() != null && Boolean.TRUE.equals(o1.getShowCustom()) && Boolean.TRUE.equals(o2.getShowCustom())) {
                    return 1;
                } else if (Boolean.TRUE.equals(o1.getShowCustom()) && Boolean.FALSE.equals(o2.getShowCustom())) {
                    return -1;
                } else if (Boolean.FALSE.equals(o1.getShowCustom()) && Boolean.FALSE.equals(o2.getShowCustom())) {
                    return 1;
                } else if (o1.getId() != null && o2.getId() != null && Boolean.TRUE.equals(o1.getShowCustom()) && Boolean.TRUE.equals(o2.getShowCustom())) {
                    return (o1.getId() - o2.getId()) > 0 ? 1 : -1;
                } else {
                    return 0;
                }
            }
        });
        return tree;
    }

    @Override
    @Transactional
    public String findPropDisplayName(String propLayRec, String modelCode) {
        Locale locale = LocaleContextHolder.getLocale();
        String[] propertyLayRec = null, firstProperty = null;
        if (null != modelCode && !modelCode.equals("")) {
            Model model = modelServiceFoundation.getModel(modelCode);
            if (null != propLayRec && !propLayRec.equals("")) {
                propertyLayRec = propLayRec.split("\\|\\|");
                firstProperty = propertyLayRec[0].split("\\.");
                if (firstProperty.length > 1) {
                    List<Model> modelList = modelServiceFoundation.getModelBycode(toUpperCaseFirstOne(firstProperty[firstProperty.length - 2]));
                    String propDisplayName = "";
                    if (modelList != null && modelList.size() > 0) {
                        propDisplayName = internationalResource.getI18nValue(modelList.get(0).getName(), locale);
                    } else {
                        String s = toUpperCaseFirstOne(firstProperty[firstProperty.length - 3]) + "_" + firstProperty[firstProperty.length - 2];
                        List<Property> properties = propertyMapper.selectList(new LambdaQueryWrapper<Property>().likeLeft(Property::getCode, s));
                        if (properties != null && properties.size() > 0) {
                            propDisplayName = internationalResource.getI18nValue(properties.get(0).getDisplayName(), locale);
                        }
                    }
                    return propDisplayName + "." + internationalResource.getI18nValue(model.getName(), locale);
                } else {
                    return internationalResource.getI18nValue(model.getName(), locale);
                }
            } else {
                return internationalResource.getI18nValue(model.getName(), locale);
            }
        }
        return null;
    }


    @Override
    @Transactional
    public List<CustomPropertyViewBO> findDgCustomePropertyByDgCode(DataGrid dg, ViewType type) {
        List<CustomPropertyViewBO> cusProps = new ArrayList<>();
        Locale locale = LocaleContextHolder.getLocale();
        if (null != dg) {
            String cfg = dg.getFullConfig();
            if (StringUtils.isEmpty(cfg)) {
                cfg = ecConfigService.getEcFullConfig(dg);
            }
            if (cfg != null && cfg.contains("<customSection><![CDATA[true]]></customSection>")) { // 包含自定义字段区域
                List<CustomPropertyViewBO> dgList = new ArrayList<>();
                CustomPropertyViewBO m = new CustomPropertyViewBO();
                String dgName = dg.getDataGridName();
                if (dgName != null && dgName.length() > 0) {
                    dgName = internationalResource.getI18nValue(dgName, locale);
                    m.setDisplayName("DataGrid[" + dgName + "]");
                } else {
                    m.setDisplayName("DataGrid[" + dg.getName() + "]");
                }
                m.setNullable(false);
                m.set_code(dg.getCode());
                m.set_parentCode(null);
                m.setIsParent(true);
                m.setLayRec(m.get_code());

                Map<String, Object> dgMap = (Map<String, Object>) SerializeUitls.deserialize(cfg);
                Map<String, Object> layout = (Map<String, Object>) dgMap.get("layout");
                if (layout != null && layout.size() > 0) {
                    List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
                    if (sections != null && sections.size() > 0) {
                        for (Map<String, Object> sec : sections) {
                            if ("DATAGRID".equals(sec.get("regionType")) || "LISTPT".equals(sec.get("regionType"))) {
                                List<Map<String, Object>> cells = (List<Map<String, Object>>) sec.get("cells");
                                if (cells != null && cells.size() > 0) {
                                    for (Map<String, Object> cell : cells) {
                                        if (cell.get("customSection") != null
                                                && cell.get("customSection").toString().equalsIgnoreCase("true")) {
                                            String modelCode = (String) cell.get("customModelCode");
                                            String propLayRec = (String) cell.get("propertyLayRec");
                                            if (modelCode != null && modelCode.length() > 0) {
                                                Model model = modelMapeer.selectById(modelCode);
//                                                CustomPropertyViewBO dgm = new CustomPropertyViewBO();
//                                                // 为了区分视图管理中datagrid的对象来源
//                                                // 通过propertyLayRec区分
//                                                dgm.setDisplayName(findPropDisplayName(propLayRec, modelCode));
//                                                dgm.setNullable(false);

//                                                dgm.set_code(dgmCode);
//                                                dgm.set_parentCode(m.get_code());
//                                                dgm.setIsParent(true);
//                                                dgm.setLayRec();
//                                                dgm.setPropertyLayRec(propLayRec);
                                                String dgmCode = model.getCode() + UUID.randomUUID().toString().replace("-", "");
                                                String layRecl = m.get_code() + "-" + dgmCode;

                                                List<CustomPropertyViewBO> list2 = generateCustomPropertyViewMappings(
                                                        model, dg.getCode(), dg.getCode(), layRecl, false, propLayRec, type);
                                                if (list2 != null && list2.size() > 0) {
                                                    String propDisplayName = findPropDisplayName(propLayRec, modelCode);
                                                    String displayName = m.getDisplayName() + "[" + propDisplayName + "]";
                                                    m.setDisplayName(displayName);
                                                    dgList.addAll(list2);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (dgList != null && dgList.size() > 0) {
                    m.setList(dgList);
                    cusProps.add(m);
//                    cusProps.addAll(dgList);
                }
            }
        }
        return cusProps;
    }

    @Override
    @Transactional
    public List<DataGrid> getDataGrids(String viewCode) {
        View view2 = getViewFromJpa(viewCode);
        if (null != view2 && view2.getIsShadow()) {
            return this.getDataGrids(view2.getShadowViewCode());
        }
        if (view2 != null) {
            return null;
        }

        return dataGridMapper.selectList(new LambdaQueryWrapper<DataGrid>().eq(DataGrid::getViewCode, view2.getScriptCode()));
    }

    private View getViewFromJpa(String viewCode) {
        return viewMapper.selectById(viewCode);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public DataGrid getDataGrid(String dataGridCode) {
        return dataGridMapper.selectOne(new LambdaQueryWrapper<DataGrid>().like(DataGrid::getCode, dataGridCode));
    }

    @Override
    @Transactional
    public void saveCustomPropertyViewMapping(CustomPropertyViewBO viewMapping) {

        LambdaQueryWrapper<CustomPropertyView> customPropertyViewMappingLambdaQueryWrapper = new LambdaQueryWrapper<>();
        customPropertyViewMappingLambdaQueryWrapper.eq(CustomPropertyView::getPropertyCode, viewMapping.getProperty().getCode())
                .eq(CustomPropertyView::getAssociatedCode, viewMapping.getAssociatedCode());
        if (viewMapping.getPropertyLayRec() != null && viewMapping.getPropertyLayRec().length() > 0) {
            customPropertyViewMappingLambdaQueryWrapper.eq(CustomPropertyView::getPropertyLayRec, viewMapping.getPropertyLayRec());
        }
        CustomPropertyView vm = customPropertyViewMappingMapper.selectOne(customPropertyViewMappingLambdaQueryWrapper);
        if (vm != null) {
            vm.setDisplayName(viewMapping.getDisplayName());
            vm.setNullable(viewMapping.getNullable());
            vm.setShowCustom(viewMapping.getShowCustom());
            vm.setColspan(viewMapping.getColspan());
            vm.setTextareaRow(viewMapping.getTextareaRow());
            vm.setReadonly(viewMapping.getReadonly());
            vm.setAlign(viewMapping.getAlign());
            vm.setPrecision(viewMapping.getPrecision());
            vm.setSort(viewMapping.getSort());
            vm.setLength(viewMapping.getLength());
        } else {
            vm = new CustomPropertyView();
            BeanUtils.copyProperties(viewMapping, vm);
            vm.setPropertyCode(viewMapping.getProperty().getCode());
            vm.setId(IDGenerator.newInstance().generate().longValue());
        }
        customPropertyViewMappingService.saveOrUpdate(vm);

    }

    @Override
    public void showProperty(List<ViewEnabledStatusCodeBO> codes, List<Long> ids, Boolean enabled) {

        if (ids != null && ids.size() > 0) {
            List<CustomPropertyView> customPropertyViewMappings = customPropertyViewMappingMapper.selectBatchIds(ids);
            for (CustomPropertyView viewMapping : customPropertyViewMappings) {
                viewMapping.setShowCustom(enabled);
                if (Boolean.FALSE.equals(enabled)) {
                    viewMapping.setSort(null);
                }
            }
            customPropertyViewMappingService.saveOrUpdateBatch(customPropertyViewMappings);
        }
        if (codes != null && codes.size() > 0) {
            for (ViewEnabledStatusCodeBO viewEnabledStatusCodeBO : codes) {
                LambdaQueryWrapper<CustomPropertyModel> eqPropertyCode = new LambdaQueryWrapper<CustomPropertyModel>()
                        .eq(CustomPropertyModel::getPropertyCode, viewEnabledStatusCodeBO.getPropertyCode());
                CustomPropertyModel modelMapping = customPropertyModelMappingMapper.selectOne(eqPropertyCode);
                if (modelMapping != null) {
                    LambdaQueryWrapper<CustomPropertyView> viewMappingLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    LambdaQueryWrapper<CustomPropertyView> eq = viewMappingLambdaQueryWrapper.eq(CustomPropertyView::getPropertyCode, viewEnabledStatusCodeBO.getPropertyCode())
                            .eq(CustomPropertyView::getAssociatedCode, viewEnabledStatusCodeBO.getAssociatedCode());
                    if (StringUtils.isNotBlank(viewEnabledStatusCodeBO.getPropertyLayRec())) {
                        eq.eq(CustomPropertyView::getPropertyLayRec, viewEnabledStatusCodeBO.getPropertyLayRec());
                    }
                    CustomPropertyView viewMapping = customPropertyViewMappingMapper.selectOne(eq);
                    if (viewMapping == null) {
                        viewMapping = new CustomPropertyView();
                        viewMapping.setId(IDGenerator.newInstance().generate().longValue());
                        viewMapping.setDisplayName(modelMapping.getDisplayName());
                        viewMapping.setFieldType(modelMapping.getFieldType());
                        viewMapping.setFormat(modelMapping.getFormat());
                        viewMapping.setNullable(modelMapping.getNullable());
                        viewMapping.setShowCustom(enabled);
                        viewMapping.setPropertyLayRec(viewEnabledStatusCodeBO.getPropertyLayRec());
                        viewMapping.setPropertyCode(modelMapping.getPropertyCode());
                        viewMapping.setAssociatedCode(viewEnabledStatusCodeBO.getAssociatedCode());
                    } else {
                        viewMapping.setShowCustom(enabled);
                        if (Boolean.FALSE.equals(enabled)) {
                            viewMapping.setSort(null);
                        }
                    }
                    customPropertyViewMappingService.saveOrUpdate(viewMapping);
                }
            }
        }

    }

    //    // 首字母转大写
    private static String toUpperCaseFirstOne(String s) {
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
        }

    }
}
