package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.configuration.services.service.rpc.MsModuleServiceApi;
import com.supcon.supfusion.configuration.services.utils.DBColumnNames;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.vo.ImportVO;
import com.supcon.supfusion.configuration.services.openapi.vo.ModelVO;
import com.supcon.supfusion.configuration.services.openapi.wrapper.ModelWrapper;
import com.supcon.supfusion.configuration.services.service.*;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;


/**
 * 导入导出配置Action
 *
 * @author zhengjiefeng
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1/ec/import")
public class EcImportController extends ConfigurationBaseController {

    private static final ModelWrapper modelWrapper = new ModelWrapper();


    @Autowired
    private EntityService entityService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private ViewService viewService;
    @Autowired
    private ImportTemplateService importTemplateService;
    @Autowired
    private ImportTemplateServiceFoundation importTemplateServiceFoundation;
    @Autowired
    private GenerateService generateService;
    @Autowired
    private ModuleService moduleService;

    /**
     * 导入导出配置页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/import/importTemplateConfig")
    public String importTemplateConfig(ModelMap map, String entityCode) {
        Entity entity = entityService.getEntity(entityCode);
        map.addAttribute("entity", entity);
        return "import/importFrame";
    }

    /**
     * 获取模型列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/import/modelList")
    public List<ModelVO> modelList(Entity entity) {
        List<Model> modelList = modelService.findModels(entity);

        return modelWrapper.e2vList(modelList);
    }

    /**
     * 保存导入导出模板
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/import/save")
    public ImportVO save(@RequestParam("model.code") String modelCode,
                       String queryConfig, boolean showCustom) throws Exception {
        ImportTemplate importTemplate = importTemplateService.getImportTemplateByCode(modelCode);
        if (importTemplate == null) {
            importTemplate = new ImportTemplate();
            importTemplate.setCode(modelCode);
        }
        String value = getQueryConfig(modelCode, queryConfig.toString(), showCustom);
        importTemplate.setValue(value);
        importTemplate.setProjFlag(false);
        importTemplateService.saveImportTemplate(importTemplate);
        return getImportRes(modelCode);
    }

    /**
     * 还原导入导出模板
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/ec/import/restore")
    public void restore(Model model) {
        ImportTemplate importTemplate = importTemplateService.getImportTemplateByCode(model.getCode());
        if (null != importTemplate) {
            importTemplateService.deleteImportTemplate(importTemplate);
        }
        //发布到runtime
        ImportTemplate ecImportTemplate = importTemplateService.getImportTemplateByHql("from ImportTemplate where  code=? ", model.getCode());
        ImportTemplate runtimeImportTemplate = importTemplateServiceFoundation.getImportTemplateByCode(model.getCode());
        if (runtimeImportTemplate != null) {
            runtimeImportTemplate.setValue(ecImportTemplate.getValue());
            importTemplateServiceFoundation.saveImportTemplate(runtimeImportTemplate);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MsModuleServiceApi msModuleServiceApi;
    /**
     * 发布导入导出模板（实体配置专用）
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/import/publish")
    public ImportVO publish(@RequestParam("model.code") String modelCode,
                            String queryConfig, boolean showCustom) throws Exception {
        //保存到ec
        ImportTemplate importTemplate = importTemplateService.getImportTemplateByCode(modelCode);
        if (importTemplate == null) {
            importTemplate = new ImportTemplate();
            importTemplate.setCode(modelCode);
        }
        String value = getQueryConfig(modelCode, queryConfig.toString(), showCustom);
        importTemplate.setValue(value);
        importTemplate.setProjFlag(false);
        importTemplateService.saveImportTemplate(importTemplate);

        //发布到runtime
        ArrayList<String> als=new ArrayList<String>();
        als.add("delete from runtime_import_template where code like '"+modelCode+"%'");
        als.add("insert into runtime_import_template (" + DBColumnNames.COLUMN_NAMES.get("IMPORT_TEMPLATE")+") select " + DBColumnNames.COLUMN_NAMES.get("IMPORT_TEMPLATE")
                + " from ec_import_template where code like '"+modelCode+"%'");
        String[] sa=new String[1];
        jdbcTemplate.batchUpdate(als.toArray(sa));
        /*ImportTemplate it = importTemplateServiceFoundation.getImportTemplateByCode(modelCode);
        if (it == null) {
            it = new ImportTemplate();
            it.setCode(modelCode);
        } else {
            if (it.getProjFlag() == null || !it.getProjFlag()) {
                it.setValue(value);
                it.setProjFlag(false);
                importTemplateServiceFoundation.saveImportTemplate(it);
            }
        }*/

        //生成文件
//        Module module = moduleService.getModule(modelService.getModel(modelCode).getModuleCode());
//        generateService.generateImportTemplateXMLInside(module);
        msModuleServiceApi.publishImportTemplate(modelCode);
        return getImportRes(modelCode);
    }

    private ImportVO getImportRes(String modelCode) {
        ImportVO importVO = new ImportVO();
        importVO.setCode(modelCode);
        return importVO;
    }

    /**
     * 获取字段
     *
     * @return
     */
    @RequestMapping(value = "/ec/import/propertyList")
    public String propertyList(ModelMap map,
                               @RequestParam(value = "model.code", required = false) String modelCode,
                               boolean showCustom) {
        boolean isSysbase = false;
        List<Map<String, String>> mapDatas = null;
        Map<String, Object> subs = null;
        if (!StringUtils.isEmpty(modelCode)) {
            isSysbase = modelCode.startsWith("sysbase_1.0");
            ImportTemplate it = importTemplateService.getImportTemplateByCode(modelCode);
            if (it != null) {
                //已选
                mapDatas = parseXml(it.getValue());
            }
            //所有
            subs = getSubEntitiesAndProperties(modelCode, "list", showCustom);
        }
        map.addAttribute("isSysbase", isSysbase);
        map.addAttribute("mapDatas", mapDatas);
        map.addAttribute("subs", subs);
        map.addAttribute("isProj", false);
        Model model = new Model();
        model.setCode(modelCode);
        map.addAttribute("model", model);
        return "import/importRight";
    }

    /**
     * 子属性
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/import/select_subs")
    public Map selectSubs(@RequestParam("model.code") String modelCode, boolean showCustom) {
        Map<String, Object> subInfos = null;
        if (!StringUtils.isEmpty(modelCode)) {
            subInfos = getExportSubEntitiesAndProperties(modelCode, "fast", showCustom);
        }
        return subInfos;

    }


    @SuppressWarnings("unchecked")
    @ResponseBody
    @RequestMapping(value = "/ec/import/getAllField")
    public Map getAllField(@RequestParam("model.code") String modelCode, boolean showCustom) {
        Map<String, Object> subs = null;
        Map<String, Object> map = new HashMap<String, Object>();
        if (!StringUtils.isEmpty(modelCode)) {
            List<Map<String, String>> data = importTemplateService.getRequireData(modelCode, true, showCustom);
            List<Map<String, String>> backData = new ArrayList<Map<String, String>>();
            //所有 assPropertyName
            subs = getSubEntitiesAndProperties(modelCode, "list", showCustom);
            List<Property> properties = (List<Property>) subs.get("properties");
            int dataSize = data.size();
            // 处理字段属性的内容
            int propertieSize = properties.size();
            for (int i = 0; i < propertieSize; i++) {
                Property tempProperty = properties.get(i);
                for (int j = 0; j < dataSize; j++) {
                    // 如果存在对象特有字段，则下一条
                    if (data.get(j).containsKey("assPropertyName")) {
                        continue;
                    }

                    // 不是对象属性，则判断字段名称是否相同
                    if (!data.get(j).containsKey("columnName") || !tempProperty.getColumnName().equalsIgnoreCase(data.get(j).get("columnName").toString())) {
                        continue;
                    }

                    backData.add(data.get(j));
                    data.remove(data.get(j));
                    dataSize--;
                    j--;
                }
            }

            // 处理对象属性的内容
            List<AssociatedInfo> associatedInfos = (List<AssociatedInfo>) subs.get("associatedInfos");
            int associatedInfoSize = associatedInfos.size();
            for (int i = 0; i < associatedInfoSize; i++) {
                AssociatedInfo tempAssociatedInfo = associatedInfos.get(i);
                for (int j = 0; j < dataSize; j++) {
                    // 如果不存在对象特有字段，则下一条
                    if (!data.get(j).containsKey("assPropertyName") || !tempAssociatedInfo.getOriginalProperty().getColumnName().equalsIgnoreCase(data.get(j).get("assPropertyName").toString())) {
                        continue;
                    }

                    backData.add(data.get(j));
                    data.remove(data.get(j));
                    dataSize--;
                    j--;
                }
            }
            backData.addAll(data);
            map.put("data", backData);
        }
        return map;
    }


    /**
     * 重新组织queryConfig
     *
     * @param modelCode
     * @param queryConfig
     * @param
     * @return
     */
    private String getQueryConfig(String modelCode, String queryConfig, Boolean showCustom) {
        Model model = modelService.getModel(modelCode);

        List<String> runningCustomPropertyCode = importTemplateService.getRunningCustomProperties(modelCode);//已启用的自定义字段
        Map<String, Object> excelRunningCustomPropertyCode = new HashMap<String, Object>();
        excelRunningCustomPropertyCode.put(modelCode, runningCustomPropertyCode);

        String supplyConfig = new String();

        List<Criterion> criterionList = new ArrayList<Criterion>();
        criterionList.add(Restrictions.eq("model.code", model.getCode()));
        criterionList.add(Restrictions.eq("valid", true));
        if (!showCustom) {
            criterionList.add(Restrictions.eq("isCustom", showCustom));
        }
        List<Property> properties = modelService.findProperties(criterionList.toArray(new Criterion[0]));

        //用于导入时条件过滤
        for (Property p : properties) {
            if (model.getIsMain()) {
                if ((p.getIsBussinessKey() || p.getIsMainDisplay()) && !p.getName().equals("id") && !queryConfig.contains(p.getCode())) {
                    supplyConfig += generteAuxXml(p);
                } else if (p.getIsCustom() && runningCustomPropertyCode.contains(p.getCode()) && !p.getNullable() && !queryConfig.contains(p.getCode())) {
                    supplyConfig += generteAuxXml(p);
                } else if (!p.getMultable() && !p.getNullable() && !p.getName().equals("id") &&
                        ((p.getIsCustom() && runningCustomPropertyCode.contains(p.getCode())) || !p.getIsCustom())) {//用于导入时非空字段必导
                    if (p.getType() != null && !p.getType().toString().equals("OBJECT") && !queryConfig.contains(p.getCode())) {
                        supplyConfig += generteAuxXml(p);
                    }
                    if (p.getType() != null && p.getType().toString().equals("OBJECT")) {
                        Model assModel = null;
                        if (!p.getIsCustom()) {
                            //String assProperty = drdcptmx1Service.getAssProperty(p.getCode());
                            //String assModelCode = drdcptmx1Service.getPropertyModelCode(assProperty);
                            //assModel = modelServiceFoundation.getModel(assModelCode);
                            assModel = p.getAssociatedProperty().getModel();
                        }

                        List<Property> assProperties = modelService.findProperties(assModel);

                        if (assProperties != null) {
                            //获取模型中Id对应的数据库列名
                            String idColumnName = "ID";
                            for (Property ap : assProperties) {
                                if (ap.getName() != null && ap.getName().equals("id")) {
                                    idColumnName = ap.getColumnName();
                                }
                            }

                            for (Property ap : assProperties) {
                                if ((ap.getIsMainDisplay() && !ap.getName().equals("id") && !queryConfig.contains(ap.getCode())) || (ap.getIsBussinessKey() && !queryConfig.contains(ap.getCode()))) {
                                    supplyConfig += generteAuxObjXml(p, ap, assModel, idColumnName, model);
                                }
                            }
                        }
                    }
                } else if (p.getName().equals("tableNo") && !queryConfig.contains(p.getCode())) {
                    supplyConfig += generteAuxXml(p);
                } else if (p.getName().equals("ownerStaff")) {//用于导出时，基础表单必导所有者
                    //String assProperty = drdcptmx1Service.getAssProperty(p.getCode());
                    //String assModelCode = drdcptmx1Service.getPropertyModelCode(assProperty);
                    //Model assModel = modelServiceFoundation.getModel(assModelCode);
                    if (null != p.getAssociatedProperty()) {
                        Model assModel = p.getAssociatedProperty().getModel();
                        if (!queryConfig.contains("ownerStaff.code")) {
                            supplyConfig += generteStaffOrPositionXml(p, assModel, "ownerStaffCode");
                        }
                        if (!queryConfig.contains("ownerStaff.name")) {
                            supplyConfig += generteStaffOrPositionXml(p, assModel, "ownerStaffName");
                        }
                    }
                } else if (p.getName().equals("ownerPosition") && !p.getModel().getEntity().getIsBase()) {//用于导出时，流程表单导出拥有者岗位
                    //String assProperty = drdcptmx1Service.getAssProperty(p.getCode());
                    //String assModelCode = drdcptmx1Service.getPropertyModelCode(assProperty);
                    //Model assModel = modelServiceFoundation.getModel(assModelCode);
                    Model assModel = p.getAssociatedProperty().getModel();
                    if (!queryConfig.contains("ownerPosition.code")) {
                        supplyConfig += generteStaffOrPositionXml(p, assModel, "ownerPositionCode");
                    }
                    if (!queryConfig.contains("ownerPosition.name")) {
                        supplyConfig += generteStaffOrPositionXml(p, assModel, "ownerPositionName");
                    }
                }
            } else {
                if (!p.getNullable() && !p.getName().equals("id") && !queryConfig.contains(p.getCode()) &&
                        ((p.getIsCustom() && runningCustomPropertyCode.contains(p.getCode())) || !p.getIsCustom())) {//用于导入时非空字段必导
                    if (p.getType() != null && !p.getType().toString().equals("OBJECT")) {
                        supplyConfig += generteAuxXml(p);
                    }
                    if (p.getType() != null && p.getType().toString().equals("OBJECT")) {
                        Model assModel = null;
                        if (!p.getIsCustom()) {
                            //String assProperty = drdcptmx1Service.getAssProperty(p.getCode());
                            //String assModelCode = drdcptmx1Service.getPropertyModelCode(assProperty);
                            //assModel = modelServiceFoundation.getModel(assModelCode);
                            assModel = p.getAssociatedProperty().getModel();
                        }

                        List<Property> assProperties = modelService.findProperties(assModel);

                        if (assProperties != null) {
                            //获取模型中Id对应的数据库列名
                            String idColumnName = "ID";
                            for (Property ap : assProperties) {
                                if (ap.getName() != null && ap.getName().equals("id")) {
                                    idColumnName = ap.getColumnName();
                                }
                            }

                            for (Property ap : assProperties) {
                                if ((ap.getIsMainDisplay() && !ap.getName().equals("id")) || ap.getIsBussinessKey()) {
                                    supplyConfig += generteAuxObjXml(p, ap, assModel, idColumnName, model);
                                }
                            }
                        }
                    }
                } else if (p.getIsMainAssociated() && !p.getName().equals("id") && p.getType().toString().equals("OBJECT") && !supplyConfig.contains(p.getCode()) && !p.getIsCustom()) {
                    Model assModel = null;
                    if (!p.getIsCustom()) {
                        //String assProperty = drdcptmx1Service.getAssProperty(p.getCode());
                        //String assModelCode = drdcptmx1Service.getPropertyModelCode(assProperty);
                        //assModel = modelServiceFoundation.getModel(assModelCode);
                        assModel = p.getAssociatedProperty().getModel();
                    }

                    List<Property> assProperties = modelService.findProperties(assModel);

                    if (assProperties != null) {
                        //获取模型中Id对应的数据库列名
                        String idColumnName = "ID";
                        for (Property ap : assProperties) {
                            if (ap.getName() != null && ap.getName().equals("id")) {
                                idColumnName = ap.getColumnName();
                            }
                        }

                        for (Property ap : assProperties) {
                            if ((ap.getIsMainDisplay() && !ap.getName().equals("id") && !queryConfig.contains(ap.getCode())) || (ap.getIsBussinessKey() && !ap.getName().equals("id") && !queryConfig.contains(ap.getCode()))) {
                                supplyConfig += generteAuxObjXml(p, ap, assModel, idColumnName, model);
                            }
                        }
                    }
                }
            }
        }

        if (supplyConfig.length() > 0) {
            queryConfig = queryConfig.substring(0, queryConfig.lastIndexOf("</list>")) + supplyConfig + "</list>";
        }

        return queryConfig;
    }


    /**
     * 拼接用于生产sql的xml
     *
     * @return
     */
    private String generteAuxXml(Property p) {
        String queryConfigAux = "<list-item>";
        queryConfigAux += "<name><![CDATA[" + (p.getName() != null ? p.getName() : "undefined") + "]]></name>";
        queryConfigAux += "<dispalyName><![CDATA[" + (p.getDisplayName() != null ? p.getDisplayName() : "undefined") + "]]></dispalyName>";
        queryConfigAux += "<propertyCode><![CDATA[" + (p.getCode() != null ? p.getCode() : "undefined") + "]]></propertyCode>";
        queryConfigAux += "<namekey><![CDATA[" + (p.getDisplayName() != null ? p.getDisplayName() : "undefined") + "]]></namekey>";
        if (p.getMultable()) {
            queryConfigAux += "<multable><![CDATA[true]]></multable>";
        } else {
            queryConfigAux += "<multable><![CDATA[false]]></multable>";
        }
        if (p.getSeniorSystemCode()) {
            queryConfigAux += "<seniorsystemcode><![CDATA[true]]></seniorsystemcode>";
        } else {
            queryConfigAux += "<seniorsystemcode><![CDATA[false]]></seniorsystemcode>";
        }
        queryConfigAux += "<propshowformat><![CDATA[" + (p.getFormat() != null ? p.getFormat() : "undefined") + "]]></propshowformat>";
        queryConfigAux += "<assPropertyName><![CDATA[undefined]]></assPropertyName>";
        queryConfigAux += "<modelCode><![CDATA[undefined]]></modelCode>";
        queryConfigAux += "<columnName><![CDATA[" + (p.getColumnName() != null ? p.getColumnName() : "undefined") + "]]></columnName>";
        queryConfigAux += "<layRec><![CDATA[" + (p.getName() != null ? p.getName() : "undefined") + "]]></layRec>";
        queryConfigAux += "<key><![CDATA[" + (p.getName() != null ? p.getName() : "undefined") + "]]></key>";
        if (p.getNullable() || p.getName().equals("id")) {
            queryConfigAux += "<nullable><![CDATA[true]]></nullable>";
        } else {
            queryConfigAux += "<nullable><![CDATA[false]]></nullable>";
        }
        queryConfigAux += "<columntype><![CDATA[" + (p.getType() != null ? p.getType() : "undefined") + "]]></columntype>";
        if (p.getDecimalNum() != null && p.getDecimalNum() > 0) {
            queryConfigAux += "<decimalNum><![CDATA[" + p.getDecimalNum().toString() + "]]></decimalNum>";
        } else {
            queryConfigAux += "<decimalNum><![CDATA[0]]></decimalNum>";
        }
        queryConfigAux += "<isCustom><![CDATA[" + p.getIsCustom() + "]]></isCustom>";
        queryConfigAux += "</list-item>";
        return queryConfigAux;
    }

    private String generteAuxObjXml(Property p, Property ap, Model assModel, String idColumnName, Model realmodel) {
        String targetColumnName = null;
        if (null != p.getAssociatedProperty() && null != p.getAssociatedProperty().getName() && !"".equals(p.getAssociatedProperty().getName())) {
            targetColumnName = p.getAssociatedProperty().getColumnName().toUpperCase();
        } else {
            targetColumnName = idColumnName;
        }
        String queryConfigAuxObj = "<list-item>";
        queryConfigAuxObj += "<name><![CDATA[" + (p.getName() != null ? p.getName() : "undefined") + "." +
                (ap.getName() != null ? ap.getName() : "undefined") + "]]></name>";
        queryConfigAuxObj += "<dispalyName><![CDATA[" + (p.getDisplayName() != null ? p.getDisplayName() : "undefined") + "," +
                (ap.getDisplayName() != null ? ap.getDisplayName() : "undefined") + "]]></dispalyName>";
        queryConfigAuxObj += "<propertyCode><![CDATA[" + (p.getCode() != null ? p.getCode() : "undefined") + "||" +
                (ap.getCode() != null ? ap.getCode() : "undefined") + "]]></propertyCode>";
        queryConfigAuxObj += "<namekey><![CDATA[" + (p.getDisplayName() != null ? p.getDisplayName() : "undefined") + "," +
                (ap.getDisplayName() != null ? ap.getDisplayName() : "undefined") + "]]></namekey>";
        if (p.getMultable()) {
            queryConfigAuxObj += "<multable><![CDATA[true]]></multable>";
        } else {
            queryConfigAuxObj += "<multable><![CDATA[false]]></multable>";
        }
        if (p.getSeniorSystemCode()) {
            queryConfigAuxObj += "<seniorsystemcode><![CDATA[true]]></seniorsystemcode>";
        } else {
            queryConfigAuxObj += "<seniorsystemcode><![CDATA[false]]></seniorsystemcode>";
        }
        queryConfigAuxObj += "<propshowformat><![CDATA[" + (ap.getFormat() != null ? ap.getFormat() : "undefined") + "]]></propshowformat>";
        queryConfigAuxObj += "<assPropertyName><![CDATA[" + (ap.getName() != null ? ap.getName() : "undefined") + "]]></assPropertyName>";
        queryConfigAuxObj += "<modelCode><![CDATA[" + assModel.getCode() + "]]></modelCode>";
        queryConfigAuxObj += "<columnName><![CDATA[" + (ap.getColumnName() != null ? ap.getColumnName() : "undefined") + "]]></columnName>";
        queryConfigAuxObj += "<layRec><![CDATA[" + assModel.getTableName() + "," + targetColumnName + "," + realmodel.getTableName() + "," + p.getColumnName() + "-" +
                (ap.getName() != null ? ap.getName() : "undefined") + "]]></layRec>";
        queryConfigAuxObj += "<key><![CDATA[" + (p.getName() != null ? p.getName() : "undefined") + "." +
                (ap.getName() != null ? ap.getName() : "undefined") + "]]></key>";
        //queryConfigAuxObj += "<key><![CDATA[" + assModel.getModelName().substring(0, 1).toLowerCase() + assModel.getModelName().substring(1) + "." +
        //		(ap.getName()!=null?ap.getName():"undefined") + "]]></key>";
        if (!p.getNullable() || ((p.getCode().contains("_ownerStaff") || p.getCode().contains("_ownerPosition")) && ap.getIsBussinessKey())) {
            queryConfigAuxObj += "<nullable><![CDATA[false]]></nullable>";
        } else {
            queryConfigAuxObj += "<nullable><![CDATA[true]]></nullable>";
        }
        queryConfigAuxObj += "<columntype><![CDATA[" + (ap.getType() != null ? ap.getType() : "undefined") + "]]></columntype>";
        if (ap.getDecimalNum() != null && ap.getDecimalNum() > 0) {//如果是小数类型，记录小数的位数
            queryConfigAuxObj += "<decimalNum><![CDATA[" + ap.getDecimalNum().toString() + "]]></decimalNum>";
        } else {
            queryConfigAuxObj += "<decimalNum><![CDATA[0]]></decimalNum>";
        }
        queryConfigAuxObj += "<isCustom><![CDATA[" + ap.getIsCustom() + "]]></isCustom>";
        queryConfigAuxObj += "</list-item>";
        return queryConfigAuxObj;
    }

    private String generteStaffOrPositionXml(Property p, Model model, String type) {
        String supplyConfig = "<list-item>";
        if (type.equals("ownerStaffCode")) {
            supplyConfig += "<name><![CDATA[ownerStaff.code]]></name>";
            supplyConfig += "<dispalyName><![CDATA[ec.common.ownerStaff,foundation.ec.entity.staff.code]]></dispalyName>";
            supplyConfig += "<propertyCode><![CDATA[" + (p.getCode() != null ? p.getCode() : "undefined") + "||base_staff_code]]></propertyCode>";
            supplyConfig += "<namekey><![CDATA[ec.common.ownerStaff,foundation.ec.entity.staff.code]]></namekey>";
            supplyConfig += "<multable><![CDATA[false]]></multable>";
            supplyConfig += "<seniorsystemcode><![CDATA[false]]></seniorsystemcode>";
            supplyConfig += "<propshowformat><![CDATA[TEXT]]></propshowformat>";
            supplyConfig += "<columnName><![CDATA[OWNER_STAFF_ID]]></columnName>";
            supplyConfig += "<layRec><![CDATA[base_staff,ID," + model.getTableName() + ",OWNER_STAFF_ID-code]]></layRec>";
            supplyConfig += "<key><![CDATA[ownerStaff.code]]></key>";
            supplyConfig += "<nullable><![CDATA[false]]></nullable>";
            supplyConfig += "<columntype><![CDATA[TEXT]]></columntype>";
            supplyConfig += "<isCustom><![CDATA[false]]></isCustom>";
            supplyConfig += "<customPropImportCode><![CDATA[undefined]]></customPropImportCode>";
        } else if (type.equals("ownerStaffName")) {
            supplyConfig += "<name><![CDATA[ownerStaff.name]]></name>";
            supplyConfig += "<dispalyName><![CDATA[ec.common.ownerStaff,foundation.staff.dimissionStaff_xls.staffName]]></dispalyName>";
            supplyConfig += "<propertyCode><![CDATA[" + (p.getCode() != null ? p.getCode() : "undefined") + "||base_staff_name]]></propertyCode>";
            supplyConfig += "<namekey><![CDATA[ec.common.ownerStaff,foundation.staff.dimissionStaff_xls.staffName]]></namekey>";
            supplyConfig += "<multable><![CDATA[false]]></multable>";
            supplyConfig += "<seniorsystemcode><![CDATA[false]]></seniorsystemcode>";
            supplyConfig += "<propshowformat><![CDATA[TEXT]]></propshowformat>";
            supplyConfig += "<columnName><![CDATA[OWNER_STAFF_ID]]></columnName>";
            supplyConfig += "<layRec><![CDATA[base_staff,ID," + model.getTableName() + ",OWNER_STAFF_ID-name]]></layRec>";
            supplyConfig += "<key><![CDATA[ownerStaff.name]]></key>";
            supplyConfig += "<nullable><![CDATA[false]]></nullable>";
            supplyConfig += "<columntype><![CDATA[TEXT]]></columntype>";
            supplyConfig += "<isCustom><![CDATA[false]]></isCustom>";
            supplyConfig += "<customPropImportCode><![CDATA[undefined]]></customPropImportCode>";
        } else if (type.equals("ownerPositionCode")) {
            supplyConfig += "<name><![CDATA[ownerPosition.code]]></name>";
            supplyConfig += "<dispalyName><![CDATA[ec.common.ownerPosition,foundation.position.code]]></dispalyName>";
            supplyConfig += "<propertyCode><![CDATA[" + (p.getCode() != null ? p.getCode() : "undefined") + "||base_position_code]]></propertyCode>";
            supplyConfig += "<namekey><![CDATA[ec.common.ownerPosition,foundation.position.code]]></namekey>";
            supplyConfig += "<multable><![CDATA[false]]></multable>";
            supplyConfig += "<seniorsystemcode><![CDATA[false]]></seniorsystemcode>";
            supplyConfig += "<propshowformat><![CDATA[TEXT]]></propshowformat>";
            supplyConfig += "<columnName><![CDATA[OWNER_POSITION_ID]]></columnName>";
            supplyConfig += "<layRec><![CDATA[BASE_POSITION,ID," + model.getTableName() + ",OWNER_POSITION_ID-code]]></layRec>";
            supplyConfig += "<key><![CDATA[ownerPosition.code]]></key>";
            supplyConfig += "<nullable><![CDATA[false]]></nullable>";
            supplyConfig += "<columntype><![CDATA[TEXT]]></columntype>";
            supplyConfig += "<isCustom><![CDATA[false]]></isCustom>";
            supplyConfig += "<customPropImportCode><![CDATA[undefined]]></customPropImportCode>";
        } else if (type.equals("ownerPositionName")) {
            supplyConfig += "<name><![CDATA[ownerPosition.name]]></name>";
            supplyConfig += "<dispalyName><![CDATA[ec.common.ownerPosition,foundation.position.name]]></dispalyName>";
            supplyConfig += "<propertyCode><![CDATA[" + (p.getCode() != null ? p.getCode() : "undefined") + "||base_position_name]]></propertyCode>";
            supplyConfig += "<namekey><![CDATA[ec.common.ownerPosition,foundation.position.name]]></namekey>";
            supplyConfig += "<multable><![CDATA[false]]></multable>";
            supplyConfig += "<seniorsystemcode><![CDATA[false]]></seniorsystemcode>";
            supplyConfig += "<propshowformat><![CDATA[TEXT]]></propshowformat>";
            supplyConfig += "<columnName><![CDATA[OWNER_POSITION_ID]]></columnName>";
            supplyConfig += "<layRec><![CDATA[BASE_POSITION,ID," + model.getTableName() + ",OWNER_POSITION_ID-name]]></layRec>";
            supplyConfig += "<key><![CDATA[ownerPosition.name]]></key>";
            supplyConfig += "<nullable><![CDATA[false]]></nullable>";
            supplyConfig += "<columntype><![CDATA[TEXT]]></columntype>";
            supplyConfig += "<isCustom><![CDATA[false]]></isCustom>";
            supplyConfig += "<customPropImportCode><![CDATA[undefined]]></customPropImportCode>";
        }
        supplyConfig += "</list-item>";
        return supplyConfig;
    }

    /**
     * 获取模型的属性和关联模型
     *
     * @return
     */
    private Map<String, Object> getExportSubEntitiesAndProperties(String modelCode, String type, boolean showCustom) {
        Map<String, Object> subs = new HashMap<String, Object>();
        Model model = modelService.getModel(modelCode);

        // 获取实体属性
        List<Property> properties = modelService.findProperties(model);

        // 获取已启用自定义字段
        List<String> runningCustomPropertyCodeList = modelService.getRunningCustomProperties(model.getCode());
        List<AssociatedInfo> associatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> oneToManyAssociatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> reverseAssociatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
        if (origAssociatedInfos != null) {
            for (AssociatedInfo asso : origAssociatedInfos) {
                // 以当前模型做为源模型的一对多关联
                //if (null != type && type.equals("fast")) {
                if ((modelCode.equals(asso.getOriginalProperty().getModel().getCode()) && null != asso.getType() && asso.getType() == 3)
                        || (modelCode.equals(asso.getTargetProperty().getModel().getCode())
                        && !asso.getTargetProperty().getModel().getCode()
                        .equals(asso.getOriginalProperty().getModel().getCode()) && asso.getTargetProperty().getModel()
                        .getCode().equals(asso.getOriginalProperty().getModel().getCode()))) {
                    asso.getOriginalProperty().setModel(model);
                    oneToManyAssociatedInfos.add(asso);
                    continue;
                }
                if (null == asso.getOriginalProperty().getAssociatedProperty() && null != asso.getTargetProperty()
                        && null != asso.getTargetProperty().getAssociatedProperty()
                        && asso.getTargetProperty().getAssociatedProperty().getCode().equals(asso.getOriginalProperty().getCode())) {
                    continue;
                }

                for (int i = 0; i < properties.size(); i++) {
                    if (properties.get(i).getIsCustom() && (!runningCustomPropertyCodeList.contains(properties.get(i).getCode()) || !showCustom)) {// 未启用自定义字段移除
                        properties.remove(i);
                    }
                }

                for (int i = 0; i < properties.size(); i++) {
                    if (properties.get(i).getDisplayName().equals(asso.getOriginalProperty().getDisplayName())) {// 关联字段移除
                        properties.remove(i);
                        break;
                    }
                }

                asso.getOriginalProperty().setModel(model);
                if (model.getInherentCommonFlag() != null
                        && model.getInherentCommonFlag()
                        && ("mainObj".equalsIgnoreCase(asso.getOriginalProperty().getName()) || "linkId".equalsIgnoreCase(asso
                        .getOriginalProperty().getName()))) {
                    continue;
                }
                associatedInfos.add(asso);
            }
        }
        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property p = it.next();
            if (p.getType() == DbColumnType.PASSWORD || p.getType() == DbColumnType.OFFICE) {
                it.remove();
                properties.remove(p);
            }

        }
        if (null != type && type.equals("fast")) {
            for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                Property p = it.next();
                if (p.getType() == DbColumnType.PASSWORD
                        || p.getType() == DbColumnType.OFFICE
                        || p.getType() == DbColumnType.TAGNUMBER
                        || p.getType() == DbColumnType.SUMMARY
                        || p.getType() == DbColumnType.PROPERTYATTACHMENT
                        || p.getType() == DbColumnType.PICTURE
                        || p.getType() == DbColumnType.COLOR
                        || p.getMultable()) {
                    it.remove();
                    properties.remove(p);
                }

            }
        }

        if (model.getIsMain() != null && model.getIsMain() && model.getEntity() != null && model.getEntity().getWorkflowEnabled() != null
                && model.getEntity().getWorkflowEnabled()) {
            List<AssociatedInfo> inherentAssos = modelService.findInherentAssociatedInfos(AssociatedInfo.ONE_TO_MANY);
            List<Property> idProperties = modelService.findProperties(Restrictions.eq("model", model), Restrictions.eq("name", "id"));
            List<Property> tableInfoIdProperties = modelService.findProperties(Restrictions.eq("model", model), Restrictions.eq("name", "tableInfoId"));
            for (AssociatedInfo item : inherentAssos) {
                if ("linkId".equals(item.getTargetProperty().getName())) {
                    item.setOriginalProperty(tableInfoIdProperties.get(0));
                } else {
                    item.setOriginalProperty(idProperties.get(0));
                }
            }
            oneToManyAssociatedInfos.addAll(inherentAssos);
        }

        subs.put("properties", properties);
        subs.put("associatedInfos", associatedInfos);
        subs.put("oneToManyAssociatedInfos", oneToManyAssociatedInfos);
        subs.put("reverseAssociatedInfos", reverseAssociatedInfos);
        //******
//		if (view != null) {
//			if (!subs.containsKey("mainEntityName")) {
//				subs.put("mainEntityName", view.getAssModel().getModelName());
//			}
//		}
        return subs;
    }

    @RequestMapping(value = "/ec/import/init")
    public String init(ModelMap map, String entityCode) {
        Entity entity = entityService.getEntity(entityCode);
        if(null!=entity&&null!=entity.getModule()){
            map.addAttribute("artifact", entity.getModule().getArtifact());
        }
        map.addAttribute("entity", entity);
        map.addAttribute("isProj", false);

        return "import/importFrame";
    }

    /**
     * 获取模型的属性和关联模型
     *
     * @return
     */
    private Map<String, Object> getSubEntitiesAndProperties(String modelCode, String type, boolean showCustom) {
        Map<String, Object> subs = new HashMap<String, Object>();
        Model model = modelService.getModel(modelCode);
        // 获取实体属性
//		List<Criterion> list=new ArrayList<Criterion>();
//		list.add(Restrictions.eq("model", model));
//		list.add(Restrictions.eq("valid", true));
//		if(!showInherent){
//			list.add(Restrictions.eq("isInherent", false));
//		}
//		if(!showCustom){
//			list.add(Restrictions.or(Restrictions.eq("isCustom", false), Restrictions.isNull("isCustom")));
//		}

        List<Property> properties = modelService.findProperties(model);
        List<AssociatedInfo> associatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfosForTemplate(model, false);
        Set<String> assoDisplayNames = null;
        if (origAssociatedInfos != null) {
            assoDisplayNames = new HashSet<String>();
            for (AssociatedInfo asso : origAssociatedInfos) {
                // 以当前模型做为源模型的一对多关联
                if (null == asso.getOriginalProperty().getAssociatedProperty() && null != asso.getTargetProperty()
                        && null != asso.getTargetProperty().getAssociatedProperty()
                        && asso.getTargetProperty().getAssociatedProperty().getCode().equals(asso.getOriginalProperty().getCode())) {
                    continue;
                }
                assoDisplayNames.add(asso.getOriginalProperty().getDisplayName());

                asso.getOriginalProperty().setModel(model);
                if (model.getInherentCommonFlag() != null
                        && model.getInherentCommonFlag()
                        && ("mainObj".equalsIgnoreCase(asso.getOriginalProperty().getName()) || "linkId".equalsIgnoreCase(asso
                        .getOriginalProperty().getName()))) {
                    continue;
                }
                associatedInfos.add(asso);
            }
        }
        List<String> propCodes = new ArrayList<String>();
        if (assoDisplayNames == null) {
            if (!ObjectUtils.isEmpty(properties)) {
                for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                    Property p = it.next();
                    //密码、offic控件、位号、摘要、附件、图片、 多选系统编码暂不支持导入 后面支持再放开
                    if (p.getType() == DbColumnType.PASSWORD
                            || p.getType() == DbColumnType.OFFICE
                            || p.getType() == DbColumnType.TAGNUMBER
                            || p.getType() == DbColumnType.SUMMARY
                            || p.getType() == DbColumnType.PROPERTYATTACHMENT
                            || p.getType() == DbColumnType.PICTURE
                            || p.getType() == DbColumnType.COLOR
                            || p.getMultable()) {
                        it.remove();
                    } else {
                        propCodes.add(p.getCode());
                    }
                }
            }
        } else {
            if (!ObjectUtils.isEmpty(properties)) {
                for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                    Property p = it.next();
                    //密码、offic控件、位号、摘要、附件、图片、多选系统编码暂不支持导入 后面支持再放开
                    if (p.getType() == DbColumnType.PASSWORD
                            || p.getType() == DbColumnType.OFFICE
                            || p.getType() == DbColumnType.TAGNUMBER
                            || p.getType() == DbColumnType.SUMMARY
                            || p.getType() == DbColumnType.PROPERTYATTACHMENT
                            || p.getType() == DbColumnType.PICTURE
                            || p.getType() == DbColumnType.COLOR
                            || p.getMultable()
                            || assoDisplayNames.contains(p.getDisplayName())) {
                        it.remove();
                    } else if (p.getIsInherent() && !"tableNo".equals(p.getName())&& !"sort".equals(p.getName())) {//固有字段在所有字段中过滤掉，不支持导入,除单据编号
                        it.remove();
                    } else {
                        propCodes.add(p.getCode());
                    }
                }
            }
        }

        if (null != type && type.equals("list")) {
            if (!ObjectUtils.isEmpty(properties)) {
                for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                    Property p = it.next();
                    if (!p.getIsUsedForList()) {
                        it.remove();
                        //properties.remove(p);
                    } else if (p.getName().equalsIgnoreCase("extraCol")) {
                        it.remove();
                        //properties.remove(p);
                    } else if (model.getCode().startsWith("sysbase_1.0") && p.getName().equals("version")) {
                        it.remove();
                    } else if (p.getIsCustom()) {
                        it.remove();
                    } else if (p.getName().equals("status") && null != model.getIsMain() && model.getIsMain() && model.getEntity().getWorkflowEnabled()) {
                        p.setType(DbColumnType.OBJECT);
                        AssociatedInfo associatedInfo = new AssociatedInfo();
                        Property statusProperty = modelService.getProperty("base_status_id");
                        associatedInfo.setTargetProperty(statusProperty);
                        associatedInfo.setOriginalProperty(p);
                        associatedInfo.setEcEnv(EcEnv.product);
                        associatedInfo.setType(1);
                        associatedInfo.setIsMainAssociated(false);
                        associatedInfo.setValid(true);
                        associatedInfo.setVersion(0);
                        associatedInfos.add(associatedInfo);
                    }

                }
            }
            //加入自定义字段(从模型管理中查找启用的字段)
            if (showCustom) {
                List<Property> propList = viewService.getEnabledCustomProps(modelCode);
                if (null != propList) {
                    List<Property> objCustom = new ArrayList<>();
                    Set<Property> customProperties = new HashSet<>();
                    for (int i = 0; i < propList.size(); i++) {
                        //自定义字段对象字段需要单独处理  多选系统编码暂不支持导入 后面支持再放开
                        if (!propList.get(i).getType().equals(DbColumnType.OBJECT) && !propList.get(i).getMultable()) { //对象类型的单独处理
                            customProperties.add(propList.get(i));
                        } else if (propList.get(i).getType().equals(DbColumnType.OBJECT)) {
                            objCustom.add(propList.get(i));
                        }
                    }
                    if (customProperties.size() > 0) {
                        properties.addAll(customProperties);
                    }
                    //单独处理对象类型
                    if (null != objCustom) {
                        List<AssociatedInfo> CustomAssociatedInfos = modelService.findAssociatedInfosForTemplate(model, showCustom);
                        for (Property objp : objCustom) {
                            for (AssociatedInfo asso : CustomAssociatedInfos) {
                                if (objp.getCode().equals(asso.getOriginalProperty().getCode()) || objp.getCode().equals(asso.getTargetProperty().getCode())) {
                                    // 以当前模型做为源模型的一对多关联
                                    if (null == asso.getOriginalProperty().getAssociatedProperty() && null != asso.getTargetProperty()
                                            && null != asso.getTargetProperty().getAssociatedProperty()
                                            && asso.getTargetProperty().getAssociatedProperty().getCode().equals(asso.getOriginalProperty().getCode())) {
                                        continue;
                                    }
                                    assoDisplayNames.add(asso.getOriginalProperty().getDisplayName());

                                    asso.getOriginalProperty().setModel(model);
                                    if (model.getInherentCommonFlag() != null
                                            && model.getInherentCommonFlag()
                                            && ("mainObj".equalsIgnoreCase(asso.getOriginalProperty().getName()) || "linkId".equalsIgnoreCase(asso
                                            .getOriginalProperty().getName()))) {
                                        continue;
                                    }
                                    if (!associatedInfos.contains(asso)) {//自定义字段如果关联主模型信息会造成重复
                                        associatedInfos.add(asso);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (null != type && type.equals("fast")) {
            for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                Property p = it.next();
                if (p.getType() == DbColumnType.OFFICE || p.getType() == DbColumnType.PROPERTYATTACHMENT) {
                    it.remove();
                    //properties.remove(p);
                }

            }
        }


        subs.put("properties", properties);
        subs.put("associatedInfos", associatedInfos);
        if (!subs.containsKey("mainEntityName") && !ObjectUtils.isEmpty(model)) {
            subs.put("mainEntityName", model.getModelName());
        }

        return subs;

    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<Map<String, String>> parseXml(String xmlSource) {
        List<Map<String, String>> maps = new ArrayList<Map<String, String>>();
        try {
            Document doc = (Document) DocumentHelper.parseText(xmlSource);
            Element root = doc.getRootElement();
            Iterator elements = root.elementIterator();
            for (Iterator iterator = elements; iterator.hasNext(); ) {
                Element element = (Element) iterator.next();
                Iterator innerIntertor = element.elementIterator();
                Map map = new HashMap();
                for (Iterator iterator2 = innerIntertor; innerIntertor.hasNext(); ) {
                    Element type2 = (Element) iterator2.next();
                    map.put(type2.getName(), type2.getText());
                }
                maps.add(map);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return maps;
    }

}
