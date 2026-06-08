package com.supcon.supfusion.configuration.services.projectapi.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.openapi.vo.ViewVO;
import com.supcon.supfusion.configuration.services.openapi.wrapper.ViewWrapper;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.service.rpc.MsModuleServiceApi;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/1/13
 */
@Slf4j
@Controller
public class ProjectViewController extends ConfigurationBaseController {
    private static final ViewWrapper viewWrapper = new ViewWrapper();

    // ~ 所需要的service =======================================================
    @Resource
    private EntityService entityService;
    @Resource
    private ViewService viewService;
    @Autowired
    private AdvQueryJsonService advQueryJsonService;
    @Autowired
    private EcDataSynchronizeService ecDataSynchronizeService;
    @Resource
    private MenuInfoService menuInfoService;
    @Autowired
    private EventService eventService;
    @Autowired
    private CustomerConditionService customerConditionService;
    @Autowired
    private ConditionService conditionService;
    @Autowired
    private DataGridService dataGridService;
    @Autowired
    private FieldService fieldService;
    @Autowired
    private MenuOperateService menuOperateService;
    @Autowired
    private MsModuleServiceApi msModuleServiceApi;
    @Autowired
    private GenerateService generateService;

    @RequestMapping(value = "/ec/engine/viewList")
    public String engineviewList(ModelMap map, @RequestParam("entity.code") String entityCode) throws Exception {
        Entity entity = entityService.getEntity(entityCode);
        map.addAttribute("entity", entity);
        return "project/engine/viewList";
    }

    /**
     * 进入视图管理列表页面
     *
     * @return
     */

    @ResponseBody
    @RequestMapping(value = "/ec/engine/getviews")
    public Page<ViewVO> list(@RequestParam("entity.code") String entityCode, Integer pageNo, Integer pageSize) throws Exception {
        Entity entity = entityService.getEntity(entityCode);
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        Page<View> page = new Page<View>(pageNo,pageSize);
        page = viewService.findViews(page, Restrictions
                .eq("entity", entity), Restrictions.or(
                Restrictions.eq("mobile", Boolean.FALSE),
                Restrictions.isNull("mobile")), Restrictions.eq("valid",
                Boolean.TRUE), Restrictions.eq("projFlag", Boolean.TRUE));
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return viewWrapper.e2vPage(page);
    }


    /**
     * 继承视图
     * @param entityCode
     * @param pageNo
     * @param pageSize
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/ec/engine/inheritView")
    public String inherit(ModelMap map,@RequestParam("entity.code") String entityCode, Integer pageNo, Integer pageSize) throws Exception {
        Entity entity = entityService.getEntity(entityCode);
        Page<View> page = new Page<View>(pageNo,pageSize);
        page = viewService.findViews(page, Restrictions
                .eq("entity", entity), Restrictions.or(
                Restrictions.eq("mobile", Boolean.FALSE),
                Restrictions.isNull("mobile")), Restrictions.eq("valid",
                Boolean.TRUE), Restrictions.isNull("projFlag"), Restrictions
                .isNull("inheritType"), Restrictions.or(
                Restrictions.eq("customFlag", Boolean.FALSE),
                Restrictions.isNull("customFlag")),Restrictions.ne("type", ViewType.DIGEST),Restrictions.eq("isShadow", Boolean.FALSE));
        map.addAttribute("viewPage",page);
        return "project/engine/inheritView";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/engine/inheritSave")
    public String inheritSave(HttpServletRequest request,@RequestParam("inheritType") Integer inheritType,@RequestParam("view.code") String viewCode) throws Exception {
        View view = viewService.getView(viewCode);
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        view.setInheritType(inheritType);
        view.setProjFlag(true);
        view.setPublishTime(null);
        if(view.getIsShadow()==true){
            view.setIsShadow(false);
            viewService.copyExtraView(view.getShadowView(), view);
            view.setShadowView(null);
            view.setExtraView(viewService.getExtraView(view));
        }
        if(inheritType==2){
            viewService.changeViewProjFlag(view, true);
        }
        view.setProjFlag(true);
        viewService.saveView(view);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(response);
    }
    @ResponseBody
    @RequestMapping(value = "/ec/engine/changeEnabled")
    public String changeEnabled(@RequestParam("viewCodes") String viewCodes,@RequestParam("enableFlag") Boolean enableFlag) throws Exception {
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        String[] viewCodeString=viewCodes.split(",");
        for (String viewCode:viewCodeString){
            View view = viewService.getView(viewCode);
            view.setProjEnabled(enableFlag);
            if((view.getType() == ViewType.LIST || view.getType() == ViewType.EXTRA) && null != view.getUrl() && !"".equals(view.getUrl())){
                showOrHideMenu(view.getUrl(),enableFlag,view);	//工程新建的视图，如果停用则隐藏视图，如果启用，则显示菜单
            }
            List<AdvQueryJson> advQueryJsons = new ArrayList<AdvQueryJson>();
            if((view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW || view.getType() == ViewType.EXTRA) && !enableFlag){
                advQueryJsons = advQueryJsonService.findAdvQueryJsons(Restrictions.eq("view", view));
            }
            viewService.saveView(view);
            if(!enableFlag){
                view.setAdvQueryJson(advQueryJsons);
                generateService.deleteProjViewHtml(view);
                ecDataSynchronizeService.forceSynchronizeECViewDataToRuntime(view.getCode());
                msModuleServiceApi.updateLayoutJson(view.getModuleCode());
                if(view.getType() == ViewType.MNECODE){
                    view.setPublishTime(null);
                    viewService.updatePublishTimeEnableFalse(view);
                }
            }else{
                if(view.getExtraView() != null && view.getPublishTime() != null){	//未发布过的视图不需要再进行发布
                    viewService.saveBackupView(view);
//                bapGenerateServiceV3.buildView(view, true);
                    // 半继承的工程期视图不用发布视图,助记码视图不用发布视图
                    if ((null == view.getInheritType() || view.getInheritType()==2) && view.getType() != ViewType.MNECODE) {
                        msModuleServiceApi.publishView(view.getCode(),true);
                    }
                    viewService.updatePublishTime(view);
                }
                ecDataSynchronizeService.synchronizeViewDataFromProj(view.getCode());
                msModuleServiceApi.updateLayoutJson(view.getModuleCode());
            }
        }
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(response);
    }


    @ResponseBody
    @RequestMapping(value = "/ec/engine/getViewEvent")
    public Map<String, Object> getViewEvent(@RequestParam("view.code") String viewCode,@RequestParam("eventType") String eventType) throws Exception {
        Map<String, Object> result = new HashMap<>();
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        View view = viewService.getView(viewCode);
        if(view.getIsShadow()){
            view=view.getShadowView();
        }
        ExtraView ev=view.getExtraView();
        if(ev!=null&&ev.getConfigMap()!=null){
            Map<String, Object> layout = (Map<String, Object>) view.getExtraView().getConfigMap().get("layout");
            String layoutCode=layout.get("layoutCode").toString();
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
            Event event=eventService.getEvent(view.getCode()+"_"+layoutCode+"_"+eventType);
            if(event!=null){
                ResponseMsg response = new ResponseMsg(true);
                result.put("function",event.getFunction());
                return result;
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        result.put("function","");
        return result;
    }
    @RequestMapping(value = "/ec/engine/halfInheritConfig")
    public String halfInheritConfig(ModelMap map,@RequestParam("view.code") String viewCode) throws Exception {
        String onloadevent= null;
        String onsaveevent = null;
        String beforeSaveStr= null;
        String afterSaveStr= null;
        String beforeSubmitStr= null;
        String afterSubmitStr= null;
        String listDGCCStr= null;
        String listPtRenderOverStr= null;
        String listPtInitStr= null;
        String listDbclickStr= null;
        Boolean selectFirstRow= null;
        Boolean isExportExcel= null;
        Boolean isFirstLoad= null;
        String listptinitevent= null;
        String listptrendoverevent= null;
        String listptdbclickevent= null;
        View ecView = viewService.getView(viewCode);
        ExtraView ecEv=ecView.getExtraView();
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        View view=viewService.getView(viewCode);
        ExtraView ev=view.getExtraView();
        List<DataGrid> dglist;
        Map<String,String> dgConfig;
        if(ev!=null&&ev.getConfigMap()!=null){
            Map<String, Object> layout = (Map<String, Object>) view.getExtraView().getConfigMap().get("layout");
            String layoutCode=layout.get("layoutCode").toString();
            Event onloadev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_onload_project");
            if(onloadev!=null){
                onloadevent=onloadev.getFunction();
            }
            Event onsaveev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_onsave_project");
            if(onsaveev!=null){
                onsaveevent=onsaveev.getFunction();
            }
            Event beforesaveev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_beforeSave");
            if(beforesaveev!=null){
                beforeSaveStr=beforesaveev.getFunction();
            }
            Event aftersaveev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_afterSave");
            if(aftersaveev!=null){
                afterSaveStr=aftersaveev.getFunction();
            }
            Event beforeSubmitev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_beforeSubmit");
            if(beforeSubmitev!=null){
                beforeSubmitStr=beforeSubmitev.getFunction();
            }
            Event afterSubmitev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_afterSubmit");
            if(afterSubmitev!=null){
                afterSubmitStr=afterSubmitev.getFunction();
            }
        }
        if(view.getType()==ViewType.LIST||view.getType()==ViewType.REFERENCE){
            CustomerCondition ccon = customerConditionService.getCustomerCondition(view);
            if(ccon!=null && ccon.getSql()!=null && ccon.getSql().length()>0){
                listDGCCStr=ccon.getSql();
            }else if(ccon!=null && ccon.getJsonCondition()!=null && ccon.getJsonCondition().length()>0){
                listDGCCStr=conditionService.toSql(ccon.getJsonCondition(), true).getSql();
            }
            if(ecEv!=null&&ecEv.getConfigMap()!=null){
                Map<String, Object> layout = (Map<String, Object>) ecEv.getConfigMap().get("layout");
                if (layout != null ){
                    String layoutCode=layout.get("layoutCode").toString();
                    if (!layout.isEmpty()) {
                        List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
                        if(null != sections) {
                            for (Map section : sections) {
                                if (section.get("regionType").toString().equals("LISTPT")) {
                                    Map<String, Object> listProperties = (Map<String, Object>) section.get("listProperty");
                                    listPtRenderOverStr=listProperties.get("renderOver")!=null?listProperties.get("renderOver").toString():"";
                                    listPtInitStr=listProperties.get("ptPageInit")!=null?listProperties.get("ptPageInit").toString():"";
                                    listDbclickStr=listProperties.get("dbcustomtextarea")!=null?listProperties.get("dbcustomtextarea").toString():"";
                                    selectFirstRow=Boolean.valueOf(listProperties.get("selectFirstRow")!=null?listProperties.get("selectFirstRow").toString():"");
                                    isExportExcel=Boolean.valueOf(listProperties.get("isExportExcel")!=null?listProperties.get("isExportExcel").toString():"");
                                    isFirstLoad=Boolean.valueOf(listProperties.get("isFirstLoad")!=null?listProperties.get("isFirstLoad").toString():"");
                                }
                            }
                        }
                    }
                    Event initev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_ptinit_project");
                    if(initev!=null){
                        listptinitevent=initev.getFunction();
                    }
                    Event rendoverev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_ptrendover_project");
                    if(rendoverev!=null){
                        listptrendoverevent=rendoverev.getFunction();
                    }
                    Event dbclickev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_ptdbclick_project");
                    if(dbclickev!=null){
                        listptdbclickevent=dbclickev.getFunction();
                    }
                    Event selectFirstRowev=eventService.getEvent(view.getCode()+"_selectFirstRow_project");
                    if(selectFirstRowev!=null){
                        selectFirstRow=Boolean.valueOf(selectFirstRowev.getFunction());
                    }
                    Event isExportExcelev=eventService.getEvent(view.getCode()+"_isExportExcel_project");
                    if(isExportExcelev!=null){
                        isExportExcel=Boolean.valueOf(isExportExcelev.getFunction());
                    }
                    Event isFirstLoadev=eventService.getEvent(view.getCode()+"_isFirstLoad_project");
                    if(isFirstLoadev!=null){
                        isFirstLoad=Boolean.valueOf(isFirstLoadev.getFunction());
                    }
                }
            }
        }else{
            List<DataGrid> tempdg=dataGridService.getDataGridByView(view, false);
            dgConfig=new HashMap<String, String>();
            dglist=new ArrayList<DataGrid>();
            List<Field> fields=fieldService.getFields(view.getCode());
            for(DataGrid dg:tempdg){
                boolean existFlag=false;
                for(Field f:fields){
                    if(f.getCode().indexOf(dg.getName())!=-1){
                        existFlag=true;
                        break;
                    }
                }
                if(!existFlag){
                    continue;
                }
                dglist.add(dg);
                CustomerCondition dgccon = customerConditionService.getCustomerCondition(dg);
                if(dgccon!=null && dgccon.getSql()!=null && dgccon.getSql().length()>0){
                    dgConfig.put(dg.getCode()+"_CustCon",dgccon.getSql());
                    map.addAttribute(dg.getCode()+"_CustCon",dgccon.getSql());
                }else if(dgccon!=null && dgccon.getJsonCondition()!=null && dgccon.getJsonCondition().length()>0){
                    dgConfig.put(dg.getCode()+"_CustCon",conditionService.toSql(dgccon.getJsonCondition(), true).getSql());
                    map.addAttribute(dg.getCode()+"_CustCon",conditionService.toSql(dgccon.getJsonCondition(), true).getSql());
                }
                if(dg.getConfigMap()!=null){
                    Map<String,Object> layout=(Map<String,Object>)dg.getConfigMap().get("layout");
                    Map<String,Object> listProperty=(Map<String,Object>)layout.get("listProperty");
                    if(listProperty==null){
                        List<Map<String, Object>> sections = (List<Map<String, Object>>) layout.get("sections");
                        if(null != sections) {
                            for (Map section : sections) {
                                if (section.get("regionType").toString().equals("LISTPT")) {
                                    listProperty=(Map<String,Object>)section.get("listProperty");
                                }
                            }
                        }
                    }
                    if(null !=listProperty && listProperty.get("renderOver")!=null){
                        dgConfig.put(dg.getCode()+"_renderOver",listProperty.get("renderOver").toString());
                        map.addAttribute(dg.getCode()+"_renderOver",listProperty.get("renderOver").toString());
                    }
                    if(null !=listProperty && listProperty.get("ptPageInit")!=null){
                        map.addAttribute(dg.getCode()+"_ptPageInit",listProperty.get("ptPageInit").toString());
                    }
                    Event projrenderev=eventService.getEvent(dg.getCode()+"_renderOver_project");
                    if(projrenderev!=null){
                        dgConfig.put(dg.getCode()+"_renderOver_project",projrenderev.getFunction());
                        map.addAttribute(dg.getCode()+"_renderOver_project",projrenderev.getFunction());
                    }
                    Event projinitev=eventService.getEvent(dg.getCode()+"_ptPageInit_project");
                    if(projinitev!=null){
                        dgConfig.put(dg.getCode()+"_ptPageInit_project",projinitev.getFunction());
                        map.addAttribute(dg.getCode()+"_ptPageInit_project",projinitev.getFunction());
                    }
                }
            }
        }

        map.addAttribute("onloadevent",onloadevent);
        map.addAttribute("onsaveevent",onsaveevent);
        map.addAttribute("beforeSaveStr",beforeSaveStr);
        map.addAttribute("afterSaveStr",afterSaveStr);
        map.addAttribute("beforeSubmitStr",beforeSubmitStr);
        map.addAttribute("afterSubmitStr",afterSubmitStr);
        map.addAttribute("listDGCCStr",listDGCCStr);
        map.addAttribute("listPtRenderOverStr",listPtRenderOverStr);
        map.addAttribute("listPtInitStr",listPtInitStr);
        map.addAttribute("listDbclickStr",listDbclickStr);
        map.addAttribute("selectFirstRow",selectFirstRow);
        map.addAttribute("isExportExcel",isExportExcel);
        map.addAttribute("isFirstLoad",isFirstLoad);
        map.addAttribute("listptinitevent",listptinitevent);
        map.addAttribute("listptrendoverevent",listptrendoverevent);
        map.addAttribute("listptdbclickevent",listptdbclickevent);
        map.addAttribute("view",view);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "project/engine/halfInheritConfig";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/engine/inheritConfigSave")
    public String inheritConfigSave(HttpServletRequest request) throws Exception {
        String onloadevent= null;
        String onsaveevent = null;
        String beforeSaveStr= null;
        String afterSaveStr= null;
        String beforeSubmitStr= null;
        String afterSubmitStr= null;
        String listDGCCStr= null;
        String listPtRenderOverStr= null;
        String listPtInitStr= null;
        String listDbclickStr= null;
        Boolean selectFirstRow= null;
        Boolean isExportExcel= null;
        Boolean isFirstLoad= null;
        String listptinitevent= null;
        String listptrendoverevent= null;
        String listptdbclickevent= null;
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        View view = viewService.getView(request.getParameter("view.code"));
        onloadevent = request.getParameter("onloadevent");
        //if(onloadevent!=null&&onloadevent.length()>0){
        saveProjectEvent("onload",customJSValidateAndReplace(onloadevent),true,view);
        //}
        onsaveevent = request.getParameter("onsaveevent");
        //if(onsaveevent!=null&&onsaveevent.length()>0){
        saveProjectEvent("onsave",customJSValidateAndReplace(onsaveevent),true,view);
        //}
        beforeSaveStr = request.getParameter("beforeSaveStr");
        if(beforeSaveStr!=null&&beforeSaveStr.length()>0){
            saveProjectEvent("beforeSave",beforeSaveStr,false,view);
        }
        afterSaveStr = request.getParameter("afterSaveStr");
        if(afterSaveStr!=null&&afterSaveStr.length()>0){
            saveProjectEvent("afterSave",afterSaveStr,false,view);
        }
        beforeSubmitStr = request.getParameter("beforeSubmitStr");
        if(beforeSubmitStr!=null&&beforeSubmitStr.length()>0){
            saveProjectEvent("beforeSubmit",beforeSubmitStr,false,view);
        }
        afterSubmitStr = request.getParameter("afterSubmitStr");
        if(afterSubmitStr!=null&&afterSubmitStr.length()>0){
            saveProjectEvent("afterSubmit",afterSubmitStr,false,view);
        }
        if(view.getType()==ViewType.LIST||view.getType()==ViewType.REFERENCE){
            listptinitevent = request.getParameter("listptinitevent");
            if(listptinitevent!=null&&listptinitevent.length()>0){
                saveProjectEvent("ptinit",listptinitevent,true,view);
            }else{
                delProjectEvent("ptinit",true,view);
            }
            listptrendoverevent = request.getParameter("listptrendoverevent");
            if(listptrendoverevent!=null&&listptrendoverevent.length()>0){
                saveProjectEvent("ptrendover",listptrendoverevent,true,view);
            }else{
                delProjectEvent("ptrendover",true,view);
            }
            listptdbclickevent = request.getParameter("listptdbclickevent");
            if(listptdbclickevent!=null&&listptdbclickevent.length()>0){
                saveProjectEvent("ptdbclick",listptdbclickevent,true,view);
            }else{
                delProjectEvent("ptdbclick",true,view);
            }
            isFirstLoad =Boolean.valueOf(request.getParameter("isFirstLoad"));
            selectFirstRow = Boolean.valueOf(request.getParameter("selectFirstRow"));
            isExportExcel = Boolean.valueOf(request.getParameter("isExportExcel"));
            saveProjectListProperty("selectFirstRow_project",selectFirstRow.toString(),view);
            saveProjectListProperty("isFirstLoad_project",isFirstLoad.toString(),view);
            saveProjectListProperty("isExportExcel_project",isExportExcel.toString(),view);
        }else{
            List<DataGrid> dglist=dataGridService.getDataGridByView(view, false);
//            Map<String,String> dgConfig = new HashMap<>();
            for(DataGrid dg:dglist){
                if(request.getParameter(dg.getCode()+"_renderOver_project")!=null){
                    saveDataGridProjectEvent("renderOver_project",customJSValidateAndReplace(request.getParameter(dg.getCode()+"_renderOver_project")),dg,view);
                }
                if(request.getParameter(dg.getCode()+"_ptPageInit_project")!=null){
                    saveDataGridProjectEvent("ptPageInit_project",customJSValidateAndReplace(request.getParameter(dg.getCode()+"_ptPageInit_project")),dg,view);
                }
            }
        }
        view.setProjFlag(true);
        viewService.saveView(view);
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/engine/inheritConfigPublish")
    public String inheritConfigPublish(HttpServletRequest request) throws Exception {
        View view = DtoUtils.getView(request);
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        inheritConfigSave(request);
        publishRetrialMenuOperate(view);
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        viewService.updatePublishTime(view);
        ecDataSynchronizeService.synchronizeViewDataFromProj(view.getCode());
        msModuleServiceApi.updateLayoutJson(view.getModuleCode());
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }
    private void publishRetrialMenuOperate(View v) {
        if (null == v.getRetrialFlag() || !v.getRetrialFlag()) {
            return;
        }
        Entity entity = v.getEntity();
        String code = entity.getCode() + "_retrial";
        MenuOperate mp = menuOperateService.getFlowList(entity.getCode());
        if (null == mp) {
            return;
        }
        List<MenuOperate> checkList = menuOperateService.getByCode(code, mp.getCompany().getId());
        if (null != checkList && checkList.size() > 0) {
            return;
        }
        MenuOperate newOp = new MenuOperate();
        newOp.setAction("retrial.action");
        newOp.setCid(mp.getCid());
        newOp.setCode(code);
        newOp.setEntityCode(entity.getCode());
        newOp.setVersion(0);
        newOp.setValid(true);
        newOp.setName("foundation.common.retrial");
        newOp.setMenuInfo(mp.getMenuInfo());
        newOp.setModule(mp.getModule());
        newOp.setNamespace(mp.getNamespace());
        newOp.setUrl(newOp.getNamespace() + "/" + newOp.getAction());
        menuOperateService.save(newOp);
    }

    private void saveDataGridProjectEvent(String type,String function,DataGrid dg,View view){
        String eventCode=dg.getCode() + "_" + type;
        Event e = eventService.getEvent(eventCode);
        if (null == e) {
            e = new Event();
            e.setVersion(0);
        }
        e.setCode(eventCode);
        e.setName(type);
        e.setFunction(function);
        e.setLayoutCode(dg.getCode());
        e.setModuleCode(view.getModuleCode());
        e.setEntityCode(view.getEntity().getCode());
        eventService.saveEvent(e);
    }
    private void saveProjectListProperty(String type,String function,View view){
        String eventCode=view.getCode() + "_" + type;
        Event e = eventService.getEvent(eventCode);
        if (null == e) {
            e = new Event();
            e.setVersion(0);
        }
        e.setCode(eventCode);
        e.setName(type);
        e.setFunction(function);
        e.setLayoutCode(type);
        e.setModuleCode(view.getModuleCode());
        e.setEntityCode(view.getEntity().getCode());
        eventService.saveEvent(e);
    }

    @SuppressWarnings("unchecked")
    private void delProjectEvent(String type,boolean needProject,View view){
        ExtraView ev=view.getExtraView();
        if(ev!=null){
            Map<String, Object> layout = (Map<String, Object>) view.getExtraView().getConfigMap().get("layout");
            String layoutCode=layout.get("layoutCode").toString();
            String eventCode=view.getCode() + "_" + layoutCode + "_"+type+(needProject?"_project":"");
            eventService.deleteEvent(eventCode);
        };
    }

    /**
     * 保存或者显示menuInfo
     * @param url
     */
    private void showOrHideMenu(String url,Boolean enableFlag,View view) {
        List<MenuInfo> menuInfos = menuInfoService.getMenuInfoByURL(url);
        if (enableFlag) {
            for (MenuInfo menuInfo : menuInfos) {
                if (menuInfo.getIsHide() || menuInfo.getAbsoluteHidden()) {
                    menuInfo.setIsHide(false);
                    menuInfo.setAbsoluteHidden(false);
                    menuInfoService.save(menuInfo);
                }
            }
        } else {
            if (null == view.getInheritType()) {
                for (MenuInfo menuInfo : menuInfos) {
                    if (!menuInfo.getIsHide() || !menuInfo.getAbsoluteHidden()) {
                        menuInfo.setIsHide(true);
                        menuInfo.setAbsoluteHidden(true);
                        menuInfoService.save(menuInfo);
                    }
                }
            }else if(view.getInheritType()==2){
                for (MenuInfo menuInfo : menuInfos) {
                    if (menuInfo.getUrl().equals(url)) {
                        menuInfo.setUrl(url.replace("/proj/","/"));
                        menuInfoService.save(menuInfo);
                    }
                }
            }
        }
    }
    @SuppressWarnings("unchecked")
    private void saveProjectEvent(String type,String function,boolean needProject,View view){
        ExtraView ev=view.getExtraView();
        if(ev!=null){
            Map<String, Object> layout = (Map<String, Object>) view.getExtraView().getConfigMap().get("layout");
            String layoutCode=layout.get("layoutCode").toString();
            String eventCode=view.getCode() + "_" + layoutCode + "_"+type+(needProject?"_project":"");
            Event e = eventService.getEvent(eventCode);
            if (null == e) {
                e = new Event();
                e.setVersion(0);
            }
            e.setCode(eventCode);
            e.setName(type+(needProject?"_project":""));
            e.setFunction(function);
            e.setLayoutCode(layoutCode);
            e.setModuleCode(view.getModuleCode());
            e.setEntityCode(view.getEntity().getCode());
            eventService.saveEvent(e);
        };
    }
    /**
     * 前台输入js代码验证替换禁止输入的字符串："1","TRUE","FALSE","true","false","undefined"
     * @param jsStr
     * @return jsStr
     */
    private String customJSValidateAndReplace(String jsStr) {
        if(!StringUtils.isEmpty(jsStr)){
            if (jsStr.indexOf("\"1\"")!=-1 ) {
                jsStr = jsStr.replace("\"1\"","\'1\'");
            }
            if (jsStr.indexOf("\"TRUE\"")!=-1) {
                jsStr = jsStr.replace("\"TRUE\"","\'TRUE\'");
            }
            if (jsStr.indexOf("\"true\"")!=-1) {
                jsStr = jsStr.replace("\"true\"","\'true\'");
            }
            if (jsStr.indexOf("\"FALSE\"")!=-1 ) {
                jsStr = jsStr.replace("\"FALSE\"","\'FALSE\'");
            }
            if (jsStr.indexOf("\"false\"")!=-1) {
                jsStr = jsStr.replace("\"false\"","\'false\'");
            }
            if (jsStr.indexOf("\"undefined\"")!=-1){
                jsStr = jsStr.replace("\"undefined\"","\'undefined\'");
            }
        }
        return jsStr;
    }

}
