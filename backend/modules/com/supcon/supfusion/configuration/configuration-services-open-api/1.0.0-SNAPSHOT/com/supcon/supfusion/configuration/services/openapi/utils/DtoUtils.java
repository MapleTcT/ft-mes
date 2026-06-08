package com.supcon.supfusion.configuration.services.openapi.utils;

import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.enums.SystemDisplayType;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.DialogType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.enums.ShowType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.service.PropertyService;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DtoUtils {
    private static String flowXML;
    private static String mobilequery;
    private static String entryUrl;
    private static String operatePowers;
    private static String keyDescs;
    @Resource
    private PropertyService propertyService;

    public static View getView(HttpServletRequest request) {
        View view = new View();
        view.setCode(request.getParameter("view.code"));
        view.setName(request.getParameter("view.name"));
        String version = request.getParameter("view.version");
        if (!StringUtils.isEmpty(version)) {
            view.setVersion(Integer.parseInt(version));
        } else {
            view.setVersion(0);
        }

        String entityCode = request.getParameter("view.entity.code");
        Entity entity = new Entity();
        entity.setCode(entityCode);
        view.setEntity(entity);
        view.setExtraView(new ExtraView());

        view.setModuleCode(request.getParameter("view.moduleCode"));
        String editViewType = request.getParameter("view.editViewType");
        if (!StringUtils.isEmpty(editViewType)) {
            view.setEditViewType(Integer.valueOf(editViewType));
        }
        String parentMenuId = request.getParameter("view.parentMenuId");
        if(!StringUtils.isEmpty(parentMenuId)){
            view.setParentMenuId(Long.parseLong(parentMenuId));
        }
        String menuName = request.getParameter("view.menuName");
        if(!StringUtils.isEmpty(menuName)){
            view.setMenuName(menuName);
        }
        view.setUsedForWorkFlow(Boolean.valueOf(request.getParameter("view.usedForWorkFlow")));
        view.setOnlyForQuery(Boolean.valueOf(request.getParameter("view.onlyForQuery")));
        view.setCustomFlag(Boolean.valueOf(request.getParameter("view.customFlag")));
        view.setMainView(Boolean.valueOf(request.getParameter("view.mainView")));
        view.setAttachmentFlag(Boolean.valueOf(request.getParameter("view.attachmentFlag")));
        view.setMainRef(Boolean.valueOf(request.getParameter("view.mainRef")));
        view.setHasAttachment(Boolean.valueOf(request.getParameter("view.hasAttachment")));
        view.setDealInfoShow(Boolean.valueOf(request.getParameter("view.dealInfoShow")));
        view.setIsControl(Boolean.valueOf(request.getParameter("view.isControl")));
        view.setImportFlag(Boolean.valueOf(request.getParameter("view.importFlag")));
        view.setRetrialFlag(Boolean.valueOf(request.getParameter("view.retrialFlag")));
        view.setIsShadow(Boolean.valueOf(request.getParameter("view.isShadow")));
        view.setIncludeChildren(Boolean.valueOf(request.getParameter("view.includeChildren")));
        view.setUsedForTree(Boolean.valueOf(request.getParameter("view.usedForTree")));
        view.setIsPrint(Boolean.valueOf(request.getParameter("view.isPrint")));
        view.setIsAudit(Boolean.valueOf(request.getParameter("view.isAudit")));
        view.setTitle(request.getParameter("view.title"));
        view.setTitle(request.getParameter("view.title"));
        String viewType = request.getParameter("view.type");
        if(!StringUtils.isEmpty(viewType)){
            view.setType(ViewType.valueOf(request.getParameter("view.type")));
        }
        view.setDisplayName(request.getParameter("view.displayName"));
        String displayName = request.getParameter("view.displayName");
        view.setEnableSimpleDealInfo(Boolean.valueOf(request.getParameter("view.enableSimpleDealInfo")));
        view.setDealInfoGroup(request.getParameter("view.dealInfoGroup"));
        view.setUrl(request.getParameter("view.url"));
        view.setScriptCode(request.getParameter("view.scriptCode"));

        View shadowView = new View();
        shadowView.setCode(request.getParameter("view.shadowView.code"));
        view.setShadowView(shadowView);
        view.setOpenType(request.getParameter("view.openType"));
        String dialogType = request.getParameter("view.dialogType");
        if(!StringUtils.isEmpty(dialogType)){
            view.setDialogType(DialogType.valueOf(dialogType));
        }
        if (!StringUtils.isEmpty(request.getParameter("view.showType"))) {
            view.setShowType(ShowType.valueOf(request.getParameter("view.showType")));
        }
        view.setLayoutCode(request.getParameter("view.layoutCode"));
        view.setAssTreeModelCode(request.getParameter("view.assTreeModelCode"));
        view.setAssTreeLayRec(request.getParameter("view.assTreeLayRec"));
        view.setAssTreePath(request.getParameter("view.assTreePath"));
        String inheritType = request.getParameter("view.inheritType");
        if(!StringUtils.isEmpty(inheritType)){
            view.setInheritType(Integer.valueOf(inheritType));
        }
        Model assModel = new Model();
        assModel.setCode(request.getParameter("view.assModel.code"));
        view.setAssModel(assModel);

        view.setIsReference(Boolean.valueOf(request.getParameter("view.isReference")));

        if (view.getIsReference() != null && view.getIsReference()) {
            View reference = new View();
            reference.setCode(request.getParameter("view.reference.code"));
//        reference.setReference(reference);
            view.setReference(reference);
            view.setReference(reference);
        }

        view.setClosePageAfterSave(Boolean.valueOf(request.getParameter("view.closePageAfterSave")));
        view.setControlPrint(Boolean.valueOf(request.getParameter("view.controlPrint")));
        view.setControlName(request.getParameter("view.controlName"));
        view.setControlSetingName(request.getParameter("view.controlSetingName"));
        view.setIsBatchControlPrint(Boolean.valueOf(request.getParameter("view.isBatchControlPrint")));
        view.setIsPermission(Boolean.valueOf(request.getParameter("view.isPermission")));
        view.setPermissionCode(request.getParameter("view.permissionCode"));
        view.setRefOperateName(request.getParameter("view.refOperateName"));
        view.setOperateUrl(request.getParameter("view.operateUrl"));
        view.setDescription(request.getParameter("view.description"));
        if (Boolean.valueOf(request.getParameter("assViewFlag"))
                && !StringUtils.isEmpty(request.getParameter("view.assView.code"))) {
            View assView =  new View();
            assView.setCode(request.getParameter("view.assView.code"));
            view.setAssView(assView);
        }
        if(request.getParameter("view.width")!=null) {
        	view.setWidth(Integer.valueOf(request.getParameter("view.width")));
        }
        if(request.getParameter("view.height")!=null) {
        	view.setHeight(Integer.valueOf(request.getParameter("view.height")));
        }
        return view;
    }

    public static View getSrcView(HttpServletRequest request) {
        View srcView = new View();
        srcView.setCode(request.getParameter("srcView.code"));
        srcView.setName(request.getParameter("srcView.name"));
        String version = request.getParameter("srcView.version");
        if (!StringUtils.isEmpty(version)) {
            srcView.setVersion(Integer.parseInt(version));
        } else {
            srcView.setVersion(0);
        }

        String entityCode = request.getParameter("srcView.entity.code");
        Entity entity = new Entity();
        entity.setCode(entityCode);
        srcView.setEntity(entity);
        srcView.setExtraView(new ExtraView());

        srcView.setModuleCode(request.getParameter("srcView.moduleCode"));
        String editViewType = request.getParameter("srcView.editViewType");
        if (!StringUtils.isEmpty(editViewType)) {
            srcView.setEditViewType(Integer.valueOf(editViewType));
        }
        String parentMenuId = request.getParameter("srcView.parentMenuId");
        if(!StringUtils.isEmpty(parentMenuId)){
            srcView.setParentMenuId(Long.parseLong(parentMenuId));
        }
        String menuName = request.getParameter("srcView.menuName");
        if(!StringUtils.isEmpty(menuName)){
            srcView.setMenuName(menuName);
        }
        srcView.setUsedForWorkFlow(Boolean.valueOf(request.getParameter("srcView.usedForWorkFlow")));
        srcView.setOnlyForQuery(Boolean.valueOf(request.getParameter("srcView.onlyForQuery")));
        srcView.setCustomFlag(Boolean.valueOf(request.getParameter("srcView.customFlag")));
        srcView.setMainView(Boolean.valueOf(request.getParameter("srcView.mainView")));
        srcView.setAttachmentFlag(Boolean.valueOf(request.getParameter("srcView.attachmentFlag")));
        srcView.setMainRef(Boolean.valueOf(request.getParameter("srcView.mainRef")));
        srcView.setHasAttachment(Boolean.valueOf(request.getParameter("srcView.hasAttachment")));
        srcView.setDealInfoShow(Boolean.valueOf(request.getParameter("srcView.dealInfoShow")));
        srcView.setIsControl(Boolean.valueOf(request.getParameter("srcView.isControl")));
        srcView.setImportFlag(Boolean.valueOf(request.getParameter("srcView.importFlag")));
        srcView.setRetrialFlag(Boolean.valueOf(request.getParameter("srcView.retrialFlag")));
        srcView.setIsShadow(Boolean.valueOf(request.getParameter("srcView.isShadow")));
        srcView.setIncludeChildren(Boolean.valueOf(request.getParameter("srcView.includeChildren")));
        srcView.setUsedForTree(Boolean.valueOf(request.getParameter("srcView.usedForTree")));
        srcView.setIsPrint(Boolean.valueOf(request.getParameter("srcView.isPrint")));
        srcView.setIsAudit(Boolean.valueOf(request.getParameter("srcView.isAudit")));
        srcView.setTitle(request.getParameter("srcView.title"));
        String viewType = request.getParameter("srcView.type");
        if(!StringUtils.isEmpty(viewType)){
            srcView.setType(ViewType.valueOf(request.getParameter("srcView.type")));
        }
        srcView.setDisplayName(request.getParameter("srcView.displayName"));
        srcView.setEnableSimpleDealInfo(Boolean.valueOf(request.getParameter("srcView.enableSimpleDealInfo")));
        srcView.setDealInfoGroup(request.getParameter("srcView.dealInfoGroup"));
        srcView.setUrl(request.getParameter("srcView.url"));
        srcView.setScriptCode(request.getParameter("srcView.scriptCode"));

        View shadowView = new View();
        shadowView.setCode(request.getParameter("srcView.shadowView.code"));
        srcView.setShadowView(shadowView);
        srcView.setOpenType(request.getParameter("srcView.openType"));
        String dialogType = request.getParameter("srcView.dialogType");
        if(!StringUtils.isEmpty(dialogType)){
            srcView.setDialogType(DialogType.valueOf(dialogType));
        }
        if (!StringUtils.isEmpty(request.getParameter("srcView.showType"))) {
            srcView.setShowType(ShowType.valueOf(request.getParameter("srcView.showType")));
        }
        srcView.setLayoutCode(request.getParameter("srcView.layoutCode"));
        srcView.setAssTreeModelCode(request.getParameter("srcView.assTreeModelCode"));
        srcView.setAssTreeLayRec(request.getParameter("srcView.assTreeLayRec"));
        srcView.setAssTreePath(request.getParameter("srcView.assTreePath"));
        String inheritType = request.getParameter("srcView.inheritType");
        if(!StringUtils.isEmpty(inheritType)){
            srcView.setInheritType(Integer.valueOf(inheritType));
        }
        Model assModel = new Model();
        assModel.setCode(request.getParameter("srcView.assModel.code"));
        srcView.setAssModel(assModel);

        srcView.setIsReference(Boolean.valueOf(request.getParameter("srcView.isReference")));

        View reference = new View();
        reference.setCode(request.getParameter("srcView.reference.code"));
//        reference.setReference(reference);
        srcView.setReference(reference);

        srcView.setClosePageAfterSave(Boolean.valueOf(request.getParameter("srcView.closePageAfterSave")));
        srcView.setControlPrint(Boolean.valueOf(request.getParameter("srcView.controlPrint")));
        srcView.setControlName(request.getParameter("srcView.controlName"));
        srcView.setControlSetingName(request.getParameter("srcView.controlSetingName"));
        srcView.setIsBatchControlPrint(Boolean.valueOf(request.getParameter("srcView.isBatchControlPrint")));
        srcView.setIsPermission(Boolean.valueOf(request.getParameter("srcView.isPermission")));
        srcView.setPermissionCode(request.getParameter("srcView.permissionCode"));
        srcView.setRefOperateName(request.getParameter("srcView.refOperateName"));
        srcView.setOperateUrl(request.getParameter("srcView.operateUrl"));
        srcView.setDescription(request.getParameter("srcView.description"));
        return srcView;
    }

    public static Property getPropertyVO(HttpServletRequest request) {
        Property property = new Property();
        if (!StringUtils.isEmpty(request.getParameter("property.version"))) {
            Integer version = Integer.parseInt(request.getParameter("property.version"));
            property.setVersion(version);
        } else {
            property.setVersion(0);
        }
        if (!StringUtils.isEmpty(request.getParameter("property.sort"))) {
            Integer sort = Integer.parseInt(request.getParameter("property.sort"));
            property.setSort(sort);
        } else {
            property.setSort(0);
        }

        Model model = new Model();
        model.setCode(request.getParameter("property.model.code"));

        if (!StringUtils.isEmpty(request.getParameter("model.enableDataAudit"))) {
            model.setEnableDataAudit(Boolean.parseBoolean(request.getParameter("model.enableDataAudit")));
        }
        property.setModel(model);

        property.setEntityCode(request.getParameter("property.entityCode"));

        property.setModuleCode(request.getParameter("property.moduleCode"));

        property.setCode(request.getParameter("property.code"));

        property.setDefaultValue(request.getParameter("property.defaultValue"));

        property.setFillcontent(request.getParameter("property.fillcontent"));

        property.setAttributes(request.getParameter("property.attributes"));

        if (!StringUtils.isEmpty(request.getParameter("property.isControl"))) {
            property.setIsControl(Boolean.parseBoolean(request.getParameter("property.isControl")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isUnique"))) {
            property.setIsUnique(Boolean.parseBoolean(request.getParameter("property.isUnique")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isHidden"))) {
            property.setIsHidden(Boolean.parseBoolean(request.getParameter("property.isHidden")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.nullable"))) {
            property.setNullable(Boolean.parseBoolean(request.getParameter("property.nullable")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.multable"))) {
            property.setMultable(Boolean.parseBoolean(request.getParameter("property.multable")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.seniorSystemCode"))) {
            property.setSeniorSystemCode(Boolean.parseBoolean(request.getParameter("property.seniorSystemCode")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.sensitive"))) {
            property.setSensitive(Boolean.parseBoolean(request.getParameter("property.sensitive")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.stretch"))) {
            property.setStretch(Boolean.parseBoolean(request.getParameter("property.stretch")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isBussinessKey"))) {
            property.setIsBussinessKey(Boolean.parseBoolean(request.getParameter("property.isBussinessKey")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isUsedMneCode"))) {
            property.setIsUsedMneCode(Boolean.parseBoolean(request.getParameter("property.isUsedMneCode")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isIndex"))) {
            property.setIsIndex(Boolean.parseBoolean(request.getParameter("property.isIndex")));
        }

        property.setOrgColumnName(request.getParameter("property.orgColumnName"));

        if (!StringUtils.isEmpty(request.getParameter("property.isGroupObject"))) {
            property.setIsGroupObject(Boolean.parseBoolean(request.getParameter("property.isGroupObject")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.onlyLeaf"))) {
            property.setOnlyLeaf(Boolean.parseBoolean(request.getParameter("property.onlyLeaf")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isUsedForList"))) {
            property.setIsUsedForList(Boolean.parseBoolean(request.getParameter("property.isUsedForList")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isCustom"))) {
            property.setIsCustom(Boolean.parseBoolean(request.getParameter("property.isCustom")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isUsedForSearch"))) {
            property.setIsUsedForSearch(Boolean.parseBoolean(request.getParameter("property.isUsedForSearch")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isIgnoreAudit"))) {
            property.setIsIgnoreAudit(Boolean.parseBoolean(request.getParameter("property.isIgnoreAudit")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.noAnalyzer"))) {
            property.setNoAnalyzer(Boolean.parseBoolean(request.getParameter("property.noAnalyzer")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isMainAssociated"))) {
            property.setIsMainAssociated(Boolean.parseBoolean(request.getParameter("property.isMainAssociated")));
        }

        if (!StringUtils.isEmpty(request.getParameter("property.isMainDisplay"))) {
            property.setIsMainDisplay(Boolean.parseBoolean(request.getParameter("property.isMainDisplay")));
        }

        property.setName(request.getParameter("property.name"));
        property.setColumnName(request.getParameter("property.columnName"));
        property.setDisplayName(request.getParameter("property.displayName"));

        if (!StringUtils.isEmpty(request.getParameter("property.type"))) {
            property.setType(Enum.valueOf(DbColumnType.class, request.getParameter("property.type")));
        }
        if (!StringUtils.isEmpty(request.getParameter("property.fieldType"))) {
            property.setFieldType(Enum.valueOf(FieldType.class, request.getParameter("property.fieldType")));
        }
        if (!StringUtils.isEmpty(request.getParameter("property.format"))) {
            property.setFormat(Enum.valueOf(ShowFormat.class, request.getParameter("property.format")));
        }
//        if(!StringUtils.isEmpty(request.getParameter("defaultValueText"))){
//            property.setDefaultValue(request.getParameter("defaultValueText"));
//        }

        if (!StringUtils.isEmpty(request.getParameter("property.maxLength"))) {
            Integer maxLength = Integer.parseInt(request.getParameter("property.maxLength"));
            property.setMaxLength(maxLength);
        }

        if (!StringUtils.isEmpty(request.getParameter("property.decimalNum"))) {
            Integer decimalNum = Integer.parseInt(request.getParameter("property.decimalNum"));
            property.setDecimalNum(decimalNum);
        }

        if (!StringUtils.isEmpty(request.getParameter("property.associatedType"))) {
            Integer associatedType = Integer.parseInt(request.getParameter("property.associatedType"));
            property.setAssociatedType(associatedType);
        }

        Property associatedProperty = new Property();
        if (!StringUtils.isEmpty("property.associatedProperty.code")) {
            associatedProperty.setCode(request.getParameter("property.associatedProperty.code"));
        } else {
            associatedProperty.setCode(request.getParameter("property_associatedProperty_code"));
        }
        property.setAssociatedProperty(associatedProperty);

        if (!StringUtils.isEmpty(request.getParameter("property.counterRuleId"))) {
            Long counterRuleId = Long.valueOf(request.getParameter("property.counterRuleId"));
            property.setCounterRuleId(counterRuleId);
        }

        property.setFetchMode(request.getParameter("property.fetchMode"));
        property.setPicWidth(request.getParameter("property.picWidth"));
        property.setPicHeight(request.getParameter("property.picHeight"));
        property.setDescription(request.getParameter("property.description"));
        property.setValid(true);
        return property;
    }

    public static Module getModuleVO(HttpServletRequest request) {
        Module module = new Module();
        if (!StringUtils.isEmpty(request.getParameter("module.version"))) {
            Integer version = Integer.parseInt(request.getParameter("module.version"));
            module.setVersion(version);
        } else {
            module.setVersion(0);
        }
        String code = request.getParameter("module.code");
        String lastVersion = request.getParameter("module.lastVersion");
        String artifact = request.getParameter("module.artifact");
        String acronym = request.getParameter("module.acronym");
        String projectVersion = request.getParameter("module.projectVersion");
        String name = request.getParameter("module.name");
        String showName = request.getParameter("international_modulename_showName");
        String category = request.getParameter("module.category");
        String moduleSelectsMultiIDs = request.getParameter("moduleSelectsMultiIDs");
        String moduleSelectsDeleteIds = request.getParameter("moduleSelectsDeleteIds");
        String moduleSelectsAddIds = request.getParameter("moduleSelectsAddIds");
        String moduleReferenceMultiIDs = request.getParameter("module.moduleReferenceMultiIDs");
        String moduleReferenceDeleteIds = request.getParameter("module.moduleReferenceDeleteIds");
        String moduleReferenceAddIds = request.getParameter("module.moduleReferenceAddIds");
        Boolean isProto = Boolean.parseBoolean(request.getParameter("module.isProto"));
        String moduleIsProto = request.getParameter("module_isProto");
        Boolean mainModule = Boolean.parseBoolean(request.getParameter("module.mainModule"));
        Boolean isNewGenerate = Boolean.parseBoolean(request.getParameter("module.isNewGenerate"));
        String moduleIsNewGenerate = request.getParameter("module.module_isNewGenerate");
        String description = request.getParameter("module.description");
        String companyIds = request.getParameter("module.companyIds");

        module.setCode(code);
        module.setLastVersion(lastVersion);
        module.setArtifact(artifact);
        module.setAcronym(acronym);
        module.setProjectVersion(projectVersion);
        module.setName(name);
        module.setCategory(category);
        module.setModuleReferencemultiselectIDs(moduleReferenceMultiIDs);
        module.setModuleReferenceDeleteIds(moduleReferenceDeleteIds);
        module.setModuleReferenceAddIds(moduleReferenceAddIds);
        module.setIsProto(isProto);
        module.setMainModule(mainModule);
        module.setIsNewGenerate(isNewGenerate);
        module.setDescription(description);
        module.setCompanyIds(companyIds);

        return module;
    }

    public static Entity getEntity(HttpServletRequest request) {
        Entity entity = new Entity();

        String version = request.getParameter("entity.version");
        String code = request.getParameter("entity.code");
        Module module = new Module();
        module.setCode(request.getParameter("entity.module.code"));
        Boolean isControl = Boolean.parseBoolean(request.getParameter("entity.isControl"));
        Boolean payCloseAttention = Boolean.parseBoolean(request.getParameter("entity.payCloseAttention"));
        Boolean crossCompanyFlag = Boolean.parseBoolean(request.getParameter("entity.crossCompanyFlag"));
        Boolean mobile = Boolean.parseBoolean(request.getParameter("entity.mobile"));
        Boolean enableRest = Boolean.parseBoolean(request.getParameter("entity.enableRest"));
        Boolean enableWs = Boolean.parseBoolean(request.getParameter("entity.enableWs"));
        Boolean enableFieldsPermissionConf = Boolean.parseBoolean(request.getParameter("entity.enableFieldsPermissionConf"));
        String entityName = request.getParameter("entity.entityName");
        String prefix = request.getParameter("entity.prefix");
        String name = request.getParameter("entity.name");
        if(StringUtils.isEmpty(name) || "".equals(name)){
            throw new EcException("名称不能为空");
        }
        //		String internationalEntitynameShowName = request.getParameter("international_entityname_showName");
        Boolean isBase = Boolean.parseBoolean(request.getParameter("entity.isBase"));

        SystemCode systemCode = new SystemCode();
        systemCode.setId(request.getParameter("entity.entityType.id"));

        Boolean groupEnabled = Boolean.parseBoolean(request.getParameter("entity.groupEnabled"));
        Boolean workflowEnabled = Boolean.parseBoolean(request.getParameter("entity.workflowEnabled"));
        String description = request.getParameter("entity.description");
        if (!StringUtils.isEmpty(version)) {
            entity.setVersion(Integer.parseInt(version));
        } else {
            entity.setVersion(0);
        }
        if (!StringUtils.isEmpty(code)) {
            entity.setCode(code);
        }
        entity.setModule(module);
        entity.setIsControl(isControl);
        entity.setPayCloseAttention(payCloseAttention);
        entity.setCrossCompanyFlag(crossCompanyFlag);
        entity.setMobile(mobile);
        entity.setEnableRest(enableRest);
        entity.setEnableWs(enableWs);
        entity.setEnableFieldsPermissionConf(enableFieldsPermissionConf);
        entity.setEntityName(entityName);
        entity.setPrefix(prefix);
        entity.setName(name);
        entity.setIsBase(isBase);
        entity.setEntityType(systemCode);
        entity.setGroupEnabled(groupEnabled);
        entity.setWorkflowEnabled(!isBase);
        entity.setDescription(description);
        return entity;

    }

    public static Model getModelVO(HttpServletRequest request) {
        Model model = new Model();

        if (!StringUtils.isEmpty(request.getParameter("model.version"))) {
            Integer version = Integer.parseInt(request.getParameter("model.version"));
            model.setVersion(version);
        } else {
            model.setVersion(0);
        }

        String code = request.getParameter("model.code");
        model.setCode(code);
        if (!StringUtils.isEmpty(request.getParameter("model.isConfigSpecial"))) {
            model.setIsConfigSpecial(Boolean.parseBoolean(request.getParameter("model.isConfigSpecial")));
        }

        if (!StringUtils.isEmpty(request.getParameter("entity.code"))) {
            Entity entity = new Entity();
            entity.setCode(request.getParameter("entity.code"));
            model.setEntity(entity);
        } else if (!StringUtils.isEmpty(request.getParameter("model.entity.code"))) {
            Entity entity = new Entity();
            entity.setCode(request.getParameter("model.entity.code"));
            model.setEntity(entity);
        }


        model.setModuleCode(request.getParameter("model.moduleCode"));

        model.setOrgTableName(request.getParameter("model.orgTableName"));

        if (!StringUtils.isEmpty(request.getParameter("model.isAndRelation"))) {
            model.setIsAndRelation(Boolean.parseBoolean(request.getParameter("model.isAndRelation")));
        }

        model.setModelName(request.getParameter("model.modelName"));

        model.setTableName(request.getParameter("model.tableName"));

        model.setName(request.getParameter("model.name"));

        if (!StringUtils.isEmpty(request.getParameter("model.dataType"))) {
            Integer dataType = Integer.parseInt(request.getParameter("model.dataType"));
            model.setDataType(dataType);
        }

        if (!StringUtils.isEmpty(request.getParameter("model.isMain"))) {
            model.setIsMain(Boolean.parseBoolean(request.getParameter("model.isMain")));
        }

        if (!StringUtils.isEmpty(request.getParameter("model.isExtraCol"))) {
            model.setIsExtraCol(Boolean.parseBoolean(request.getParameter("model.isExtraCol")));
        }

        if (!StringUtils.isEmpty(request.getParameter("model.isCache"))) {
            model.setIsCache(Boolean.parseBoolean(request.getParameter("model.isCache")));
        }

        if (!StringUtils.isEmpty(request.getParameter("model.enableSync"))) {
            model.setEnableSync(Boolean.parseBoolean(request.getParameter("model.enableSync")));
        }

        if (!StringUtils.isEmpty(request.getParameter("model.type"))) {
            model.setType(Integer.valueOf(request.getParameter("model.type")));
        }

        model.setDescription(request.getParameter("model.description"));

        if (model.getType() != null && model.getType() == Model.TYPE_SQL) {
            SqlModel sqlModel = new SqlModel();
            sqlModel.setCode(model.getCode());
            if (!StringUtils.isEmpty(request.getParameter("model.sqlModel.version"))) {
                Integer version = Integer.parseInt(request.getParameter("model.sqlModel.version"));
                sqlModel.setVersion(version);
            } else {
                sqlModel.setVersion(0);
            }
            sqlModel.setModelSql(request.getParameter("model.sqlModel.sql"));
            sqlModel.setProperties(request.getParameter("model.sqlModel.properties"));
            model.setSqlModel(sqlModel);
        }
        return model;
    }

    public static ExtraView getExtraView(HttpServletRequest request) {
        ExtraView ev = new ExtraView();

        ev.setCode(request.getParameter("ev.code"));

        View view = new View();
        view.setCode(request.getParameter("ev.view.code"));
        ev.setView(view);

        String version = request.getParameter("ev.version");
        if (!StringUtils.isEmpty(version)) {
            ev.setVersion(Integer.parseInt(version));
        } else {
            ev.setVersion(0);
        }
        String config = request.getParameter("ev.config");
        if (!StringUtils.isEmpty(config)) {
            ev.setConfig(config);
        }

        return ev;
    }

    public static FastQueryJson getFastQueryJson(HttpServletRequest request) {
        FastQueryJson fqj = new FastQueryJson();

        fqj.setCode(request.getParameter("fqj.code"));

        if (null != request.getParameter("fqj.view.code")) {
            View view = new View();
            view.setCode(request.getParameter("fqj.view.code"));
            fqj.setView(view);
        }

        String version = request.getParameter("fqj.version");
        if (!StringUtils.isEmpty(version)) {
            fqj.setVersion(Integer.parseInt(version));
        } else {
            fqj.setVersion(0);
        }
        String queryConfig = request.getParameter("fqj.queryConfig");
        if (!StringUtils.isEmpty(queryConfig)) {
            fqj.setQueryConfig(queryConfig);
        }

        return fqj;
    }

    public static AdvQueryJson getAdvQueryJson(HttpServletRequest request) {
        AdvQueryJson aqj = new AdvQueryJson();

        aqj.setCode(request.getParameter("aqj.code"));
        if (null != request.getParameter("aqj.view.code")) {
            View view = new View();
            view.setCode(request.getParameter("aqj.view.code"));
            aqj.setView(view);
        }
        String version = request.getParameter("aqj.version");
        if (!StringUtils.isEmpty(version)) {
            aqj.setVersion(Integer.parseInt(version));
        } else {
            aqj.setVersion(0);
        }
        String queryConfig = request.getParameter("aqj.queryConfig");
        if (!StringUtils.isEmpty(queryConfig)) {
            aqj.setQueryConfig(queryConfig);
        }

        return aqj;
    }

    public static SystemEntity getSystemEntity(HttpServletRequest request) {
        SystemEntity systemEntity = new SystemEntity();
        if (!StringUtils.isEmpty(request.getParameter("systemEntity.id"))) {
            systemEntity.setId(Long.valueOf(request.getParameter("systemEntity.id")));
        }
        systemEntity.setCid(Long.valueOf(request.getParameter("systemEntity.cid")));
        systemEntity.setCode(request.getParameter("systemEntity.code"));
        systemEntity.setName(request.getParameter("systemEntity.name"));
        if (null != request.getParameter("systemEntity.listType")) {
            systemEntity.setListType(SystemDisplayType.valueOf(request.getParameter("systemEntity.listType").toLowerCase()));
        }
        systemEntity.setModuleCode(request.getParameter("systemEntity.moduleCode"));
        systemEntity.setMemo(request.getParameter("systemEntity.memo"));
        String version = request.getParameter("systemEntity.version");
        if (!StringUtils.isEmpty(version)) {
            systemEntity.setVersion(Integer.parseInt(version));
        } else {
            systemEntity.setVersion(0);
        }
        return systemEntity;
    }

    public static Echarts getEchartsVO(HttpServletRequest request) {
        Echarts echarts = new Echarts();

        if (!StringUtils.isEmpty(request.getParameter("echarts.projFlag"))) {
            echarts.setProjFlag(Boolean.parseBoolean(request.getParameter("echarts.projFlag")));
        }
        echarts.setCode(request.getParameter("echarts.code"));
        String version = request.getParameter("echarts.version");
        if (!StringUtils.isEmpty(version)) {
            echarts.setVersion(Integer.parseInt(version));
        } else {
            echarts.setVersion(0);
        }
        echarts.setTitle(request.getParameter("echarts.title"));
        echarts.setLegendPosition(request.getParameter("echarts.legendPosition"));

        if (!StringUtils.isEmpty(request.getParameter("echarts.isFirstLoad"))&&"on".equals(request.getParameter("echarts.isFirstLoad"))) {
            echarts.setIsFirstLoad(true);
        }else{
            echarts.setIsFirstLoad(false);
        }
        if (!StringUtils.isEmpty(request.getParameter("echarts.isShowMagicType"))&&"on".equals(request.getParameter("echarts.isShowMagicType"))) {
            echarts.setIsShowMagicType(true);
        }else{
            echarts.setIsShowMagicType(false);
        }
        if (!StringUtils.isEmpty(request.getParameter("echarts.isShowLegend"))&&"on".equals(request.getParameter("echarts.isShowLegend"))) {
            echarts.setIsShowLegend(true);
        }else{
            echarts.setIsShowLegend(false);
        }
        if (!StringUtils.isEmpty(request.getParameter("echarts.modelList[0].xaxis.name"))) {
            EchartsModel emodel =new EchartsModel();
            EchartsXAxis xAxis =new EchartsXAxis();
            xAxis.setName(request.getParameter("echarts.modelList[0].xaxis.name"));
            emodel.setXaxis(xAxis);
            List<EchartsModel> modelList =new ArrayList<EchartsModel>();
            modelList.add(emodel);
            echarts.setModelList(modelList);
        }
        return echarts;
    }

    public static DataGroup getDataGroupVO(HttpServletRequest request) {
        DataGroup dataGroup = new DataGroup();

        String version = request.getParameter("dataGroup.version");
        if (!StringUtils.isEmpty(version)) {
            dataGroup.setVersion(Integer.parseInt(version));
        } else {
            dataGroup.setVersion(0);
        }
        if (!StringUtils.isEmpty(request.getParameter("dataGroup.code"))) {
            dataGroup.setCode(request.getParameter("dataGroup.code"));
        }
        View view = new View();
        view.setCode(request.getParameter("dataGroup.view.code"));
        dataGroup.setView(view);
        dataGroup.setName(request.getParameter("dataGroup.name"));
        dataGroup.setDisplayName(request.getParameter("dataGroup.displayName"));
        if (!StringUtils.isEmpty(request.getParameter("dataGroup.isMultiple"))) {
            Boolean isMultiple = Boolean.parseBoolean(request.getParameter("dataGroup.isMultiple"));
            dataGroup.setIsMultiple(isMultiple);
        }

        return dataGroup;
    }

    public static DataGrid getDataGridVO(HttpServletRequest request) {
        DataGrid dataGrid = new DataGrid();

        dataGrid.setCode(request.getParameter("dataGrid.code"));
        dataGrid.setDataGridName(request.getParameter("dataGrid.dataGridName"));
        dataGrid.setConfig(request.getParameter("dataGrid.config"));
        String version = request.getParameter("dataGrid.version");
        if (!StringUtils.isEmpty(version)) {
            dataGrid.setVersion(Integer.parseInt(version));
        } else {
            dataGrid.setVersion(0);
        }
        if (!StringUtils.isEmpty(request.getParameter("dataGrid.dataGridType"))) {
            dataGrid.setDataGridType(Integer.valueOf(request.getParameter("dataGrid.dataGridType")));
        }
        if (!StringUtils.isEmpty(request.getParameter("dataGrid.isPermission"))) {
            dataGrid.setIsPermission(Boolean.valueOf(request.getParameter("dataGrid.isPermission")));
        }
        if (!StringUtils.isEmpty(request.getParameter("dataGrid.permissionCode"))) {
            dataGrid.setPermissionCode(request.getParameter("dataGrid.permissionCode"));
        }
        if (!StringUtils.isEmpty(request.getParameter("dataGrid.operateName"))) {
            dataGrid.setOperateName(request.getParameter("dataGrid.operateName"));
        }

        return dataGrid;
    }

    public static DataClassific getDataClassificVO(HttpServletRequest request) {
        DataClassific dataClassific = new DataClassific();
        String version = request.getParameter("dataClassific.version");
        if (!StringUtils.isEmpty(version)) {
            dataClassific.setVersion(Integer.parseInt(version));
        } else {
            dataClassific.setVersion(0);
        }
        if (!StringUtils.isEmpty(request.getParameter("dataClassific.code"))) {
            dataClassific.setCode(request.getParameter("dataClassific.code"));
        }
        if (!StringUtils.isEmpty(request.getParameter("dataClassific.isDefault"))) {
            dataClassific.setIsDefault(Boolean.parseBoolean(request.getParameter("dataClassific.isDefault")));
        }
        String dataGroupCode = request.getParameter("dataClassific.dataGroup.code");
        DataGroup dataGroup = new DataGroup();
        dataGroup.setCode(dataGroupCode);
        dataClassific.setDataGroup(dataGroup);
        dataClassific.setName(request.getParameter("dataClassific.name"));
        dataClassific.setDisplayName(request.getParameter("dataClassific.displayName"));
        if (!StringUtils.isEmpty(request.getParameter("dataClassific.condition"))) {
            dataClassific.setCondition(request.getParameter("dataClassific.condition"));
        }

        return dataClassific;
    }

    public static Deployment getDeploymentVO(HttpServletRequest request) {
        Deployment deployment = new Deployment();
        ResponseMsg response = null;
        if (!StringUtils.isEmpty(request.getParameter("deploymentId"))) {
            String deploymentId = request.getParameter("deploymentId");
            deployment.setDeploymentId(deploymentId);
        }
        String version = request.getParameter("version");
        if (!StringUtils.isEmpty(version)) {
            deployment.setVersion(Integer.parseInt(version));
        } else {
            deployment.setVersion(0);
        }
        String flowKey = request.getParameter("flowKey");

        String flowName = request.getParameter("flowName");

        String namekey = request.getParameter("namekey");
        deployment.setName(namekey);

        if (!StringUtils.isEmpty(request.getParameter("menuId"))) {
            Long menuId = Long.valueOf(request.getParameter("menuId"));
            deployment.setMenuInfoId(menuId);
        }

        BigDecimal requiredTime = BigDecimal.valueOf(Long.parseLong(request.getParameter("requiredTime")));
        if (null != requiredTime) {
            if (requiredTime.compareTo(new BigDecimal(0)) == 0) {
                deployment.setRequiredTime(null);
            } else {
                deployment.setRequiredTime(requiredTime);
            }
        }

        Boolean mobileinitiate = Boolean.parseBoolean(request.getParameter("mobileinitiate"));
        Boolean mobileapprove = Boolean.parseBoolean(request.getParameter("mobileapprove"));
        String allowInvalid = request.getParameter("allowInvalid");
        String graduallyReject = request.getParameter("graduallyReject");
        Boolean recallAble = Boolean.parseBoolean(request.getParameter("recallAble"));
        String recallRemainTime = request.getParameter("recallRemainTime");
        String mainViewViewCode = request.getParameter("mainViewViewCode");
        Boolean signature = Boolean.parseBoolean(request.getParameter("signature"));
        String des = request.getParameter("des");
        String entityCode = request.getParameter("entityCode");
        String moduleCode = request.getParameter("moduleCode");

        Integer flowEditFlag = null;
        if (!StringUtils.isEmpty(request.getParameter("flowEditFlag"))) {
            flowEditFlag = Integer.parseInt(request.getParameter("flowEditFlag"));
        }

        if (null != flowXML && !"".equals(flowXML)) {
            deployment.setTempProcessXml(flowXML);
        }
        if (null != entityCode) {
            deployment.setEntityCode(entityCode);
        }
        deployment.setProcessKey(flowKey);

        if (null != mobilequery) {
            if (mobilequery.equals("true")) {
                deployment.setMobilequery(true);
            } else {
                deployment.setMobilequery(false);
            }
        }
        if (null != mobileinitiate) {
            if (mobileinitiate.equals("true")) {
                deployment.setMobileinitiate(true);
            } else {
                deployment.setMobileinitiate(false);
            }
        }
        if (null != mobileapprove) {
            if (mobileapprove.equals("true")) {
                deployment.setMobileapprove(true);
            } else {
                deployment.setMobileapprove(false);
            }
        }
        if (null != recallAble) {
            if (recallAble.equals("true")) {
                deployment.setRecallAble(true);
            } else {
                deployment.setRecallAble(false);
            }
        }

        if (null != signature) {
            deployment.setSignatureEnable(signature);
        }

        if (null != recallRemainTime && !recallRemainTime.equals("")) {
            deployment.setRecallRemainTime(Long.parseLong(recallRemainTime));
        } else {
            deployment.setRecallRemainTime(null);
        }

        if (null != mainViewViewCode && !mainViewViewCode.isEmpty()) {
            deployment.setMainViewViewCode(mainViewViewCode);
        }

        if (null != des) {
            deployment.setDescription(des);
        }
        if (null != entryUrl) {
            deployment.setEntryUrl(entryUrl);
        }
        if (null != operatePowers) {
            deployment.setOperatePowers(operatePowers);
        }
        if (null != flowEditFlag) {
            deployment.setFlowEditFlag(flowEditFlag == 1);
        }
        if (deployment.getId() == null) {
            deployment.setCreateStaffId(getCurrentStaff().getId());
            deployment.setCreateTime(new Date());
        } else {
            deployment.setModifyStaffId(getCurrentStaff().getId());
            deployment.setModifyTime(new Date());
        }

        // 保存设置允许管理员作废
        if (null != allowInvalid) {
            deployment.setAllowInvalid("true".equals(allowInvalid) ? true : false);
        }

        // 保存设置逐级驳回
        if (null != graduallyReject) {
            deployment.setGraduallyReject("true".equals(graduallyReject) ? true : false);
        }

        if (null != keyDescs) {
            deployment.setKeyDescs(keyDescs);
        }
        return deployment;
    }

    public static Staff getCurrentStaff() {
        Staff staff = new Staff();
        staff.setId(UserContext.getUserContext().getStaffId());
        staff.setCode(UserContext.getUserContext().getStaffCode());
        staff.setName(UserContext.getUserContext().getStaffName());
        return staff;
    }

}
