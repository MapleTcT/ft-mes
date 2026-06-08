package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.service.ConditionService;
import com.supcon.supfusion.configuration.services.service.DataGridService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
@Slf4j
@Controller
public class AdvQueryController extends ConfigurationBaseController {
    @Resource
    private ViewService viewService;
    @Autowired(required = true)
    private ConditionService conditionService;
    @Autowired
    private DataGridService dataGridService;
    @Resource
    private ModelService modelService;
    @ResponseBody
    @RequestMapping(value = "/ec/advQuery/select-subs")
    public Map<String, Object> selectSubs(@RequestParam(value ="noCheckModelCodes",required = false) String noCheckModelCodes, @RequestParam(value ="view.code",required = false) String viewCode , @RequestParam(value ="dataGrid.code",required = false) String dataGridCode, @RequestParam(value ="modelCode",required = false) String modelCode ) {
        View view =null;
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(null != isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        DataGrid dataGrid=null;
        Map<String, Object> subs;
        if(null!=viewCode&&!viewCode.isEmpty()){
            view= viewService.getView(viewCode);
            modelCode = view.getAssModel().getCode();
        }
        if(null!=dataGridCode&&!dataGridCode.isEmpty()){
            dataGrid=dataGridService.getDataGrid(dataGridCode);
            modelCode = dataGrid.getTargetModel().getCode();
        }
        subs=getSubEntitiesAndProperties(modelCode,view,dataGrid);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return subs;
    }


    private Map<String, Object> getSubEntitiesAndProperties(String modelCode,View view,DataGrid dataGrid) {
        long currentTime = System.currentTimeMillis();
        Map<String, Object> subs = new HashMap<String, Object>();
        log.debug("获取实体属性(ms)-start:" + (System.currentTimeMillis() - currentTime));
        Model model = modelService.getModel(modelCode);
        // 获取实体属性
        List<Property> properties = modelService.findProperties(model);
        log.debug("获取实体属性(ms)-end:" + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();
        Iterator<Property> pIt = properties.iterator();
        Property iteratorItem = null;
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model,  Property.ONE_TO_ONE,
                Property.ONE_TO_MANY, Property.MANY_TO_ONE);
        log.debug("获取关联(ms):" + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();
        List<AssociatedInfo> associatedInfos = new ArrayList<AssociatedInfo>();
        if (origAssociatedInfos != null) {
            for (AssociatedInfo asso : origAssociatedInfos) {
                // asso = modelService.getAssociatedInfoWithPropertiesAndModels(asso.getId());
                if (modelCode.equals(asso.getTargetProperty().getModel().getCode()) && !asso.getTargetProperty().getModel().getCode().equals(asso.getOriginalProperty().getModel().getCode())) {
                    asso = resetOriginalAndTarget(asso.getTargetProperty(), asso.getOriginalProperty());
                }

                // asso.getOriginalProperty().setModel(model);
                // 判断是否有读取model的权限
               /* if ((env == null || env.equals("runtime"))) {
                    if (noCheckModelCodes != null
                            && (noCheckModelCodes.contains(asso.getOriginalProperty().getModel().getCode()) || noCheckModelCodes
                            .contains(asso.getTargetProperty().getModel().getCode()))) {
                        // do nothing
                    } else if (!getPermissionModelCodes().contains(asso.getOriginalProperty().getModel().getCode())
                            || !getPermissionModelCodes().contains(asso.getTargetProperty().getModel().getCode())) {
                        continue;
                    }
                }*/
                associatedInfos.add(asso);
            }
        }
        while (pIt.hasNext()) {
            iteratorItem = pIt.next();
            if (!iteratorItem.getIsUsedForList() && !"status".equalsIgnoreCase((iteratorItem.getName()))) {
                pIt.remove();
                continue;
            }
            // fix:如果字段有关联，并且对于目标模型没有相应关联，字段也不应该出现
            if (DbColumnType.OBJECT.equals(iteratorItem.getType())) {
                boolean permittedFlag = false;
                // fix qc-1456 1457  目前未发现新版高级查询哪里用了
                for (AssociatedInfo asso : associatedInfos) {
                    if (asso.getOriginalProperty().getCode() != null && asso.getOriginalProperty().getCode().equals(iteratorItem.getCode())) {
                        permittedFlag = true;
                        break;
                    }
                }
                if (!permittedFlag) {
                    pIt.remove();
                    continue;
                }
            }
        }
        if (view != null && view.getCode() != null && !view.getCode().isEmpty()) {
            if (model.getIsMain() != null && model.getIsMain() && model.getEntity() != null
                    && model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
                List<AssociatedInfo> inherentAssos = modelService.findInherentAssociatedInfos( AssociatedInfo.ONE_TO_MANY);
                Property idProperty = modelService.findPropertyByCode(model.getCode() + "_id");
                Property tableInfoIdProperty = modelService.findPropertyByCode(model.getCode() + "_tableInfoId");
                if (idProperty != null && tableInfoIdProperty != null && inherentAssos != null) {
                    for (AssociatedInfo item : inherentAssos) {
                        if ("linkId".equals(item.getTargetProperty().getName())) {
                            item.setOriginalProperty(tableInfoIdProperty);
                        } else {
                            item.setOriginalProperty(idProperty);
                        }
                    }
                    associatedInfos.addAll(inherentAssos);
                }
            }
        }
        // 查找自定义字段
        List<Property> customProps = viewService.getEnabledCustomProps(modelCode);
        if (customProps != null && customProps.size() > 0) {
            for (Property p : customProps) {
                properties.add(p);
                if (DbColumnType.OBJECT.equals(p.getType())) {
                    AssociatedInfo ai = new AssociatedInfo();
                    ai.setOriginalProperty(p);
                    ai.setTargetProperty(p.getAssociatedProperty());
                    ai.setType(p.getAssociatedType());
                    associatedInfos.add(ai);
                }
            }
        }

        subs.put("advQueryProperties", properties);
        subs.put("advQueryAssociatedInfos", associatedInfos);
        if (view != null && view.getAssModel() != null && view.getAssModel().getModelName() != null) {
            subs.put("modelAlias", StringUtils.firstLetterToLower(view.getAssModel().getModelName()));
            view = null;
        } else if (dataGrid != null && dataGrid.getTargetModel() != null && dataGrid.getTargetModel().getModelName() != null) {
            subs.put("modelAlias", StringUtils.firstLetterToLower(dataGrid.getTargetModel().getModelName()));
            view = null;
        }

        log.debug("处理(ms):" + (System.currentTimeMillis() - currentTime));
        currentTime = System.currentTimeMillis();
        return subs;
    }

    /**
     * 重置关连的源和目标
     *
     * @return AssociatedInfo
     * @throws Exception
     */
    private AssociatedInfo resetOriginalAndTarget(Property originalProp,Property targetProp) {
        AssociatedInfo asso = new AssociatedInfo();
        asso.setOriginalProperty(originalProp);
        asso.setTargetProperty(targetProp);
        return asso;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/advQuery/select-exists-conds")
    public List<AdvQueryCondition> selectExistsConds(@RequestParam(value ="view.code",required = false) String viewCode ) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(null != isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<AdvQueryCondition> advQueryConditions=null;
        if (viewCode != null && viewCode.length() > 0) {
            advQueryConditions = conditionService.getAdvQueryConditionByView(viewCode);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return advQueryConditions;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/advQuery/reference-infos")
    public Map<String, Object> selectExistsConds(HttpServletRequest request ) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(null != isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        Map<String, Object> referencesInfo = new HashMap<String, Object>();
        String modelCode=request.getParameter("modelCode");
        List<View> viewList = viewService.findViewsByAssModelCode(modelCode, ViewType.REFERENCE);
        boolean isMobile = false;
        /*if(OrchidUtils.isMobileRequest(request)){
            isMobile = true;
        }*/
        boolean viewFlag=false;
        for (View view : viewList) {
            if(null!=view.getMainRef()&&view.getMainRef()){
                viewFlag=true;
                break;
            }
            if("/organization/#/reference?type=staff".equals(view.getUrl())){
                view.setUrl("/msService/ec/foundation/staff/common/staffListFrameset");
            }else if("/organization/#/reference?type=department".equals(view.getUrl())){
                view.setUrl("/msService/ec/foundation/department/common/departmentListFrame");
            }else if("/organization/#/reference?type=position".equals(view.getUrl())){
                view.setUrl("/msService/ec/foundation/position/common/positionListFrame");
            }else if("/organization/#/reference?type=company".equals(view.getUrl())){
                view.setUrl("/msService/ec/foundation/company/common/companyListFrame");
            }
        }
        if(viewFlag){
            for (Iterator<View> it = viewList.iterator(); it.hasNext();) {
                View view = it.next();
                if(null==view.getMainRef()||!view.getMainRef()){
                    it.remove();
                }
            }
        }
        if(isMobile){
            for (Iterator<View> it = viewList.iterator(); it.hasNext();) {
                View view = it.next();
                if(!view.getMobile()){
                    it.remove();
                }
            }
        }else{
            for (Iterator<View> it = viewList.iterator(); it.hasNext();) {
                View view = it.next();
                if(view.getMobile()){
                    it.remove();
                }
            }
        }
        referencesInfo.put("viewList", viewList);

        Model model = modelService.getModel(modelCode);
        referencesInfo.put("model", model);

        Property property = modelService.findMainDisplayProperty(model);
        referencesInfo.put("mainDisplayProperty", property);
        Property pkProperty = modelService.findPKProperty(modelCode);
        referencesInfo.put("pkProperty", pkProperty);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return referencesInfo;
    }
}
