/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * <p>
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.*;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.*;
import com.supcon.supfusion.configuration.services.openapi.wrapper.*;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.service.rpc.MsModuleServiceApi;
import com.supcon.supfusion.configuration.services.utils.ConditionUtil;
import com.supcon.supfusion.configuration.services.utils.JsonUtils;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;

/**
 * 视图管理Action类<br>
 *
 * @author songjiawei
 * @version 1.0
 */
@Slf4j
@Controller
public class ViewController extends ConfigurationBaseController {

    private static final ViewWrapper viewWrapper = new ViewWrapper();
    private static final PropertyWrapper propertyWrapper = new PropertyWrapper();
    private static final ModelWrapper modelWrapper = new ModelWrapper();
    private static final DataGroupWrapper dataGroupWrapper = new DataGroupWrapper();
    private static final ExtraViewWrapper extraViewWrapper = new ExtraViewWrapper();
    private static final FastQueryJsonWrapper fastQueryJsonWrapper = new FastQueryJsonWrapper();
    private static final AdvQueryJsonWrapper advQueryJsonWrapper = new AdvQueryJsonWrapper();


    public static final int DIRECTION_DEV_TO_RUNTIME = 1;
    public static final int DIRECTION_DEV_TO_PROJ = 2;
    public static final int DIRECTION_PROJ_TO_RUNTIME = 3;
    private static final String layerSystemEntityCode = "BASE_CONFIG_LAYERS";
    // ~ 所需要的service =======================================================
    @Resource
    private ViewService viewService;
    @Resource
    private EntityService entityService;
    @Resource
    private ModelService modelService;

    @Autowired
    private GenerateService generateService;
    @Resource
    private SystemCodeInfoService systemCodeInfoService;
    @Autowired
    private LayoutService layoutService;
    @Autowired
    private DataGridService dataGridService;
    @Autowired
    private InternationalService internationalService;
    @Resource
    private ModuleService moduleService;
    @Resource
    private ModuleReferenceService moduleReferenceService;
    @Autowired
    private FieldService fieldService;
    @Autowired
    private ButtonService buttonService;
    @Autowired
    private FastQueryJsonService fastQueryJsonService;
    @Autowired
    private AdvQueryJsonService advQueryJsonService;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private SystemCodeService systemCodeService;
    @Resource
    private PropertyService propertyService;
    @Autowired
    private EcDataSynchronizeService ecDataSynchronizeService;
    @Autowired
    private MsModuleServiceApi msModuleServiceApi;
    @Autowired
    private ActionViewService actionViewService;

    private static volatile Map<String, String> i18KeyReplaceMap = new HashMap<String, String>();

    private static final Map<String, String> viewTypes = new LinkedHashMap<String, String>();
    private static final Map<String, String> fieldTypes = new LinkedHashMap<String, String>();
    private static final Map<String, String> workFlowElements = new LinkedHashMap<String, String>();
    private static final Map<String, String> fastfieldtypes = new LinkedHashMap<String, String>();
    private static final Map<String, String> viewColumnTypes = new LinkedHashMap<String, String>();
    private static final Map<String, String> viewFormats = new LinkedHashMap<String, String>();
    private static final String STATUS_CODE = "base_status_id";

    static {
        for (DbColumnType type : DbColumnType.values()) {
            viewColumnTypes.put(type.toString(), type.getValue());
        }
        for (WorkFlowElements el : WorkFlowElements.values()) {
            workFlowElements.put(el.toString(), el.getValue());
        }

        EnumMap<ShowFormat, String> showFormats = new EnumMap<ShowFormat, String>(
                ShowFormat.class);
        showFormats.put(ShowFormat.TEXT, "ec.property.text");
        showFormats.put(ShowFormat.EMAIL, "ec.property.email");
        showFormats.put(ShowFormat.URL, "ec.property.url");
        showFormats.put(ShowFormat.IP, "ec.property.ip");

        showFormats.put(ShowFormat.PERCENT, "ec.property.percent");
        // showFormats.put(ShowFormat.MONEY, "ec.property.currency");
        showFormats.put(ShowFormat.SYSTEMCODE, "ec.property.systemcode");
        // showFormats.put(ShowFormat.ENUMERATE, "ec.property.enumerate");
        showFormats.put(ShowFormat.SELECT, "ec.property.select");
        showFormats.put(ShowFormat.CHECKBOX, "ec.property.checkbox");
        showFormats.put(ShowFormat.RADIO, "ec.property.redio");

        showFormats.put(ShowFormat.THOUSAND, "ec.property.thousand");
        showFormats.put(ShowFormat.TEN_THOUSAND, "ec.property.ten_thousand");

        showFormats.put(ShowFormat.Y, "2000");
        showFormats.put(ShowFormat.YM, "2000-05");
        showFormats.put(ShowFormat.YMD, "2000-05-01");
        showFormats.put(ShowFormat.YMD_H, "2000-05-01 06");
        showFormats.put(ShowFormat.YMD_HM, "2000-05-01 06:09");
        showFormats.put(ShowFormat.YMD_HMS, "2000-05-01 06:09:06");
        showFormats.put(ShowFormat.HM, "06:09");
        showFormats.put(ShowFormat.HMS, "06:09:06");

        showFormats.put(ShowFormat.SELECTCOMP, "ec.property.selectcomp");
        showFormats.put(ShowFormat.OFFICE, "ec.property.officeplugin");

        showFormats.put(ShowFormat.RADIO, "ec.view.property.radio");
        showFormats.put(ShowFormat.SELECTTAGNUMBER,
                "ec.property.selecttagnumber");
        showFormats.put(ShowFormat.LAYER, "ec.property.layer");
        //showFormats.put(ShowFormat.COLOR, "ec.property.color");
        showFormats.put(ShowFormat.HEX, "HEX");
        showFormats.put(ShowFormat.RGBA, "RGBA");
        showFormats.put(ShowFormat.HSLA, "HSLA");
        for (ShowFormat f : ShowFormat.values()) {
            if (null != f.toString() && !("".equals(f.toString()))) {
                if (showFormats.get(f) != null) {
                    viewFormats.put(f.toString(), showFormats.get(f));
                }
            }
        }

        EnumMap<ViewType, String> viewTypeEnums = new EnumMap<ViewType, String>(
                ViewType.class);
        viewTypeEnums.put(ViewType.EDIT, "ec.view.property.editview");
        viewTypeEnums.put(ViewType.VIEW, "ec.view.property.view");
        viewTypeEnums.put(ViewType.LIST, "ec.view.property.listview");
        // viewTypeEnums.put(ViewType.SEARCH, "查找视图");
        // viewTypeEnums.put(ViewType.ADVSEARCH, "高级查找视图");
        viewTypeEnums.put(ViewType.REFERENCE, "ec.view.property.refview");
        viewTypeEnums.put(ViewType.MNECODE, "ec.view.property.mneview");
        viewTypeEnums.put(ViewType.DIGEST, "ec.view.property.digestview");
        viewTypeEnums.put(ViewType.TREE, "ec.view.property.tree");
        viewTypeEnums.put(ViewType.REFTREE, "ec.view.property.reftree");
        viewTypeEnums.put(ViewType.EXTRA, "ec.view.property.extra");
        // viewTypeEnums.put(ViewType.LAYOUT, "组合布局视图");
        // viewTypeEnums.put(ViewType.CUSTOM, "自定义视图");

        for (ViewType type : ViewType.values()) {
            viewTypes.put(type.toString(), viewTypeEnums.get(type));
        }

        EnumMap<FieldType, String> fieldTypeEnums = new EnumMap<FieldType, String>(
                FieldType.class);
        fieldTypeEnums.put(FieldType.TEXTFIELD, "ec.view.property.text");
        fieldTypeEnums
                .put(FieldType.PASSWORDFIELD, "ec.view.property.password");
        fieldTypeEnums.put(FieldType.TEXTAREA, "ec.view.property.longtext");
        fieldTypeEnums.put(FieldType.SELECT, "ec.view.property.select");
        fieldTypeEnums.put(FieldType.DATE, "ec.view.property.date");
        fieldTypeEnums.put(FieldType.DATETIME, "ec.view.property.datetime");
        fieldTypeEnums
                .put(FieldType.MULTSELECT, "ec.view.property.multiselect");
        fieldTypeEnums.put(FieldType.MULTFILES, "ec.view.property.multifiles");
        // fieldTypeEnums.put(FieldType.OUTERSELECT,
        // "ec.view.property.outerselect");
        // fieldTypeEnums.put(FieldType.AUTOCOMPLETE, "ec.view.property.auto");
        fieldTypeEnums.put(FieldType.RICHTEXT, "ec.view.property.richtext");
        fieldTypeEnums.put(FieldType.RADIO, "ec.view.property.radioField");
        fieldTypeEnums
                .put(FieldType.CHECKBOX, "ec.view.property.checkboxField");
        fieldTypeEnums.put(FieldType.LABEL, "ec.view.property.label");
        fieldTypeEnums.put(FieldType.SELECTCOMP, "ec.view.property.selectcomp");
        fieldTypeEnums.put(FieldType.DATAGRID, "ec.view.property.datagrid");
        fieldTypeEnums.put(FieldType.EASYTABLE, "ec.view.property.easytable");
        fieldTypeEnums.put(FieldType.MULTIDATAGRID,
                "ec.view.property.multidatagrid");
        fieldTypeEnums.put(FieldType.PROPERTYATTACHMENT,
                "ec.view.property.attachment");
        fieldTypeEnums.put(FieldType.PICTURE, "ec.property.picture");
        fieldTypeEnums.put(FieldType.OFFICE, "ec.view.property.officeplugin");
        fieldTypeEnums.put(FieldType.OUTDATA, "ec.view.property.outdata");
        fieldTypeEnums.put(FieldType.SELECTTAGNUMBER,
                "ec.property.selecttagnumber");
        fieldTypeEnums.put(FieldType.LAYER, "ec.property.layerComp");
        fieldTypeEnums.put(FieldType.TIME, "ec.property.time");
        fieldTypeEnums.put(FieldType.COLOR, "ec.property.color");
        // fieldTypeEnums.put(FieldType.DATATABLE, "数据列表");
        // fieldTypeEnums.put(FieldType.DATATABLE, "数据列表");

        for (FieldType type : FieldType.values()) {
            fieldTypes.put(type.toString(), fieldTypeEnums.get(type));
        }
    }

    static {
        EnumMap<FastFieldType, String> fastFieldTypeEnums = new EnumMap<FastFieldType, String>(FastFieldType.class);
        fastFieldTypeEnums.put(FastFieldType.TEXTFIELD, "ec.view.property.text");
        fastFieldTypeEnums.put(FastFieldType.SELECT, "ec.view.property.select");
        fastFieldTypeEnums.put(FastFieldType.DATE, "ec.view.property.date");
        fastFieldTypeEnums.put(FastFieldType.DATETIME, "ec.view.property.datetime");
        // fastFieldTypeEnums.put(FastFieldType.MULTSELECT,
        // "ec.view.property.checkboxField");
        fastFieldTypeEnums.put(FastFieldType.RADIO, "ec.view.property.radio");
        fastFieldTypeEnums.put(FastFieldType.CHECKBOX, "ec.view.property.checkbox");
        fastFieldTypeEnums.put(FastFieldType.LABEL, "ec.view.property.label");
        fastFieldTypeEnums.put(FastFieldType.SELECTCOMP, "ec.view.property.selectWidget");
        fastFieldTypeEnums.put(FastFieldType.SELECTTAGNUMBER, "ec.property.selecttagnumber");
        fastFieldTypeEnums.put(FastFieldType.TIME, "ec.property.time");
        for (FastFieldType type : FastFieldType.values()) {
            fastfieldtypes.put(type.toString(), fastFieldTypeEnums.get(type));
        }
    }

    /**
     * 进入视图管理主框架
     *
     * @return
     */
    @RequestMapping(value = "/ec/view/manage")
    public String manage(ModelMap map, @RequestParam("entity.code") String entityCode) throws Exception {
        Entity entity = entityService.getEntity(entityCode);
        map.addAttribute("entity", entity);
        return "view/manage";
    }

    /**
     * 进入视图管理列表页面
     *
     * @return
     */

    @ResponseBody
    @RequestMapping(value = "/ec/view/list")
    public Page<ViewVO> list(@RequestParam("entity.code") String entityCode,Integer pageNo, Integer pageSize) throws Exception {
        Entity entity = entityService.getEntity(entityCode);
        Page<View> page = new Page<View>(pageNo,pageSize);
        page.setResult(viewService.findViews(entity, 1, 4, 0));
        return viewWrapper.e2vPage(page);
    }

    /**
     * 进入视图管理编辑页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/view/edit")
    public String edit(ModelMap map, HttpServletRequest request) throws Exception {
        Entity entity = new Entity();
        entity.setCode(request.getParameter("entity.code"));
        View view = new View();
        view.setCode(request.getParameter("view.code"));
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(null!=isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (entity != null && entity.getCode() != null
                && entity.getCode().length() > 0) {
            entity = entityService.getEntity(entity.getCode());
        }
        List<Model> assModels = null;
        List<View> assViews = null;
        if (null != request.getParameter("view.code")) {
            view = viewService.getView(view.getCode());
            entity = view.getEntity();
            assModels = modelService.findModels(entityService.getEntity(view
                    .getEntity().getCode()));
            if (null != entity) {
                assViews = viewService.findViews(entity, 1, 4, 0);
                for (Iterator<View> it = assViews.iterator(); it.hasNext(); ) {
                    View v = it.next();
                    if (v.getType().equals(ViewType.LIST)
                            && (v.getShowType().equals(ShowType.PART) || v
                            .getShowType().equals(ShowType.SINGLE))) {
                        continue;
                    } else {
                        it.remove();
                    }
                }
            }
        } else {
            assModels = modelService.findModels(entityService.getEntity(entity.getCode()));
            if (null != entityService.getEntity(entity.getCode())) {
                assViews = viewService.findViews(
                        entityService.getEntity(entity.getCode()), 1, 4, 0);
                for (Iterator<View> it = assViews.iterator(); it.hasNext(); ) {
                    View v = it.next();
                    if (v.getType().equals(ViewType.LIST)
                            && (v.getShowType().equals(ShowType.PART) || v
                            .getShowType().equals(ShowType.SINGLE))) {
                        continue;
                    } else {
                        it.remove();
                    }
                }
            }
        }
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("isProject", false);
        List<Layout> layouts = layoutService.findAll();
        map.addAttribute("responseMap", responseMap);
        map.addAttribute("layouts", layouts);
        map.addAttribute("assModels", assModels);
        map.addAttribute("assViews", assViews);
        map.addAttribute("viewTypes", viewTypes);
        map.addAttribute("isProj", isProj);
        map.addAttribute("refviews", assViews);
        map.addAttribute("entity", entity);
        if (!StringUtils.isEmpty(request.getParameter("view.code"))) {
            map.addAttribute("view", view);
        }
        if (isProj && (view == null || view.getInheritType() == null)) {
            MenuInfo menusTree = menuInfoService.getMenusTree();
            map.addAttribute("menusTree", menusTree);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/edit";
    }

    /**
     * 进入视图管理编辑页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/view/selectList")
    public String selectList(ModelMap map,@RequestParam("view.code") String viewCode) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(null !=isProj &&isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View view = viewService.getView(viewCode, true);
        ExtraView ev = view.getExtraView();
        if (null != ev) {
            if (view.getType() != ViewType.MNECODE
                    && view.getShowType() != ShowType.LAYOUT) {
                /* 已经配置过 */

                String evConfig = viewService.getExtraViewFullConfig(view);
                ev.setConfig(evConfig);
                log.info("===" + evConfig);
                Map configMap = (Map) SerializeUitls.deserialize(evConfig);
                ev.setConfigMap(configMap);
            }
        } else {
            /* 尚未配置过,提供初始信息 */
            ev = viewService.defaultExtraView(view);
        }
        List<Button> buttons = view.getButtons();
        if(null!=buttons){
            for (Button button : buttons) {
                if (null != button.getDisplayName()
                        && !"".equals(button.getDisplayName())) {
                    Thread.sleep(100);
                    String newKey = internationalService
                            .createNewInternational(button.getDisplayName());
                    i18KeyReplaceMap.put(button.getDisplayName(), newKey);
                }
            }
        }
        map.addAttribute("ev", ev);
        map.addAttribute("i18KeyReplaceMap", i18KeyReplaceMap);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/selectList";
    }

    /**
     * 进入视图copy页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/view/copyInit")
    public String copyInit(ModelMap map, @RequestParam("srcView.code") String code) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(null !=isProj &&isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View srcView = new View();
        srcView.setCode(code);
        if (null != srcView && null != srcView.getCode()) {
            View view = srcView = viewService.getView(srcView.getCode());
            Entity entity = srcView.getEntity();
            map.addAttribute("view", view);
            map.addAttribute("srcView", view);
            map.addAttribute("entity", entity);
        }
        map.addAttribute("layouts", layoutService.findAll());
        map.addAttribute("viewTypes", viewTypes);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/copy";
    }

    /**
     * 进入移动视图配置页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/configMobileInit")
    public ResponseMsg configMobileInit(HttpServletRequest request) throws Exception {
        View view = new View();
        view.setCode(request.getParameter("view.code"));
        ResponseMsg response = new ResponseMsg();
        ObjectMapper mapper = new ObjectMapper();
        if (null != view && null != view.getCode()) {
            View mobileView = viewService.getView(view.getCode()
                    + View.MOBILE_VIEW_SUFFIX);
            if (mobileView == null) {
                /*
                 * if (mobileView != null) {
                 * viewService.deleteViewPhysical(mobileView.getCode(), true); }
                 */
                mobileView = viewService.getView(view.getCode());
                mobileView.setName(mobileView.getName() + View.MOBILE_VIEW_SUFFIX);
                /*
                 * boolean isExtra = false; if((view.getType() == ViewType.EDIT
                 * || view.getType() == ViewType.VIEW || view.getType() ==
                 * ViewType.EXTRA) && view.getEditViewType() == 1){ isExtra =
                 * true; } if(isExtra){
                 */
                viewService.copyView(view, mobileView, false);
                // }else{
                // viewService.copyView(view, mobileView, true);
                // }
                view.setType(mobileView.getType());
                view.setShadowView(mobileView.getShadowView());
                mobileView = viewService.getView(view.getCode()
                        + View.MOBILE_VIEW_SUFFIX);
                mobileView.setMobile(true);
                mobileView.setMobileEnableFlag(false);
                mobileView.setMainView(false);
                mobileView.setUsedForWorkFlow(false);
                if (null != view.getShadowView()) {
                    View mobileshadowView = viewService.getView(view.getShadowView().getCode()
                            + View.MOBILE_VIEW_SUFFIX);
                    if (null != mobileshadowView) {
                        mobileView.setShadowView(mobileshadowView);
                    } else {
                        mobileView.setShadowView(null);
                    }
                }
                if (view.getType() == ViewType.EDIT
                        || view.getType() == ViewType.VIEW) {
                    mobileView.setEditViewType(0);
                } else {
                    mobileView.setEditViewType(1);
                }
                /*
                 * if(isExtra){ mobileView.setEditViewType(0); }
                 */
                viewService.saveView(mobileView);
            }
            shadowMobileView(view);
            response.setSuccess(true);
        }
        return response;
    }

    /**
     * 设置影子视图的移动视图 shadowView view 影子视图
     */
    private void shadowMobileView(View view) {
        List<View> views = viewService.getViewsByHql(view);
        if (views != null && views.size() > 0) {
            for (View viewshadow : views) {
                View mobileShadowView = viewService.getView(viewshadow
                        .getCode() + View.MOBILE_VIEW_SUFFIX);
                if (mobileShadowView == null) {
                    mobileShadowView = viewService
                            .getView(viewshadow.getCode());
                    mobileShadowView.setName(viewshadow.getName()
                            + View.MOBILE_VIEW_SUFFIX);
                    viewService.copyView(viewshadow.getShadowView(),
                            mobileShadowView, false);
                    mobileShadowView = viewService.getView(viewshadow.getCode()
                            + View.MOBILE_VIEW_SUFFIX);
                    mobileShadowView.setIsShadow(true);
                    mobileShadowView.setShadowView(viewService
                            .getView(viewshadow.getShadowView().getCode()
                                    + View.MOBILE_VIEW_SUFFIX));
                    mobileShadowView.setMobile(true);
                    mobileShadowView.setMobileEnableFlag(false);
                    mobileShadowView.setMainView(false);
                    mobileShadowView.setUsedForWorkFlow(false);
                    mobileShadowView.setEditViewType(viewService.getView(
                            viewshadow.getShadowView().getCode()
                                    + View.MOBILE_VIEW_SUFFIX)
                            .getEditViewType());
                    viewService.saveView(mobileShadowView);
                }
            }
        }
    }

    /**
     * 进入视图管理编辑页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/save")
    public String save(HttpServletRequest request, boolean assViewFlag) throws Exception {
        View view = DtoUtils.getView(request);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(null !=isProj &&isProj){
            view.setProjFlag(true);
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (!view.getRetrialFlag()) {
            view.setScriptCode("");
        }
        // if(view.getControlPrint() && (null == view.getControlName() ||
        // view.getControlName().trim().length() == 0)){
        // view.setControlName("ec.print.controlPrint");
        // view.setControlSetingName("ec.print.controlSetting");
        // }
        if (view.getOpenType() != null && view.getOpenType().equals("dialog")) {
            if (view.getDialogType() == DialogType.DIALOG_1) {
                view.setWidth(410);
                view.setHeight(270);
            } else if (view.getDialogType() == DialogType.DIALOG_2) {
                view.setWidth(350);
                view.setHeight(310);
            } else if (view.getDialogType() == DialogType.DIALOG_3) {
                view.setWidth(510);
                view.setHeight(430);
            } else if (view.getDialogType() == DialogType.DIALOG_4) {
                view.setWidth(700);
                view.setHeight(430);
            } else if (view.getDialogType() == DialogType.DIALOG_5) {
                view.setWidth(800);
                view.setHeight(550);
            }
        }
        if (!assViewFlag) {
            view.setAssView(null);
        }
        if (view.getType() != ViewType.REFERENCE
                && view.getType() != ViewType.REFTREE
                && view.getType() != ViewType.VIEW) {
            view.setIsPermission(false);
            view.setPermissionCode(null);
            view.setOperateUrl(null);
            if (view.getType() == ViewType.EXTRA) {
                view.setEditViewType(1);
            } else if ((view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW)
                    && view.getIsShadow() != null
                    && view.getIsShadow()
                    && view.getShadowView() != null) {
                View shadowView = viewService.getView(view.getShadowView()
                        .getCode());
                view.setEditViewType(shadowView.getEditViewType());
            }

            if (view.getType() != ViewType.EXTRA
                    && view.getType() != ViewType.EDIT
                    && view.getType() != ViewType.VIEW) {
                view.setEditViewType(0);
            }
            if (view.getEditViewType() == 1) {
                view.setShowType(ShowType.SINGLE);
            }
            if ((view.getIsShadow() == null || !view.getIsShadow())
                    && view.getShadowView() != null) {
                view.setShadowView(null);
            }
        } else {
            if (view.getIsPermission() && view.getType() != ViewType.VIEW) {
                if (null == view.getPermissionCode()
                        || view.getPermissionCode().trim().length() == 0) {
                    view.setPermissionCode(view.getName());
                }
                if (null == view.getRefOperateName()
                        || view.getRefOperateName().trim().length() == 0) {
                    view.setRefOperateName(view.getTitle());
                }
            }
        }
        if (null == view.getDataGridType()) {
            view.setDataGridType(0);
        }
        if (!(view.getType() == ViewType.EXTRA
                || view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW)) {
            view.setEditViewType(0); // 非编辑、查看、增强型视图，增强型标志置为0
        }

        viewService.saveView(view);
        if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW
                || view.getType() == ViewType.LIST
                || view.getType() == ViewType.REFERENCE) {
            View mobileView = viewService.getView(view.getCode()
                    + View.MOBILE_VIEW_SUFFIX);
            if (view.getIsShadow() != null && view.getIsShadow()
                    && view.getShadowView() != null) {
                View shadowMobileView = viewService.getView(view
                        .getShadowView().getCode() + View.MOBILE_VIEW_SUFFIX);
                if (shadowMobileView != null && shadowMobileView.getMobile()) {
                    // View mobileView = viewService.getView(view.getCode() +
                    // View.MOBILE_VIEW_SUFFIX);
                    if (mobileView == null) {
                        mobileView = viewService.getView(view.getCode());
                        mobileView.setName(view.getName()
                                + View.MOBILE_VIEW_SUFFIX);
                        viewService.copyView(view.getShadowView(), mobileView,
                                false);
                        mobileView = viewService.getView(view.getCode()
                                + View.MOBILE_VIEW_SUFFIX);
                        mobileView.setIsShadow(true);
                        mobileView.setShadowView(viewService.getView(view
                                .getShadowView().getCode()
                                + View.MOBILE_VIEW_SUFFIX));
                        mobileView.setMobile(true);
                        mobileView.setMobileEnableFlag(false);
                        mobileView.setMainView(false);
                        mobileView.setUsedForWorkFlow(false);
                        mobileView.setEditViewType(viewService.getView(
                                view.getShadowView().getCode()
                                        + View.MOBILE_VIEW_SUFFIX)
                                .getEditViewType());
                        mobileView.setReference(view.getReference());
                        mobileView.setIsReference(view.getIsReference());
                        mobileView.setAssModel(view.getAssModel());
                        viewService.saveView(mobileView);
                    } else if (!mobileView.getIsShadow()) {
                        mobileView.setIsShadow(true);
                        mobileView.setShadowView(viewService.getView(view
                                .getShadowView().getCode()
                                + View.MOBILE_VIEW_SUFFIX));
                        mobileView.setReference(view.getReference());
                        mobileView.setIsReference(view.getIsReference());
                        mobileView.setAssModel(view.getAssModel());
                        viewService.mergeView(mobileView);
                    }
                }
            } else {
                // View mobileView = viewService.getView(view.getCode() +
                // View.MOBILE_VIEW_SUFFIX);
                if (mobileView != null) {
                    mobileView.setIsShadow(false);
                    mobileView.setShadowView(null);
                    mobileView.setReference(view.getReference());
                    mobileView.setIsReference(view.getIsReference());
                    mobileView.setAssModel(view.getAssModel());
                    viewService.mergeView(mobileView);
                }
            }

        }
        if(isProj){
            view.setProjFlag(true);
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(response);
    }

    /**
     * 视图拷贝
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/copy")
    public ResponseMsg copy(HttpServletRequest request) throws Exception {
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if (isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View srcView = new View();
        srcView.setCode(getRequest().getParameter("srcView.code"));
        View view = DtoUtils.getView(request);
        Entity entity = new Entity();
        entity.setCode(getRequest().getParameter("view.entity.code"));
        view.setEntity(entity);
        view.setModuleCode(getRequest().getParameter("view.moduleCode"));
        view.setName(getRequest().getParameter("view.name"));
        view.setTitle(getRequest().getParameter("view.title"));
        view.setDisplayName(getRequest().getParameter("view.displayName"));
        view.setType(ViewType.getViewType(getRequest().getParameter("view.type")));

        viewService.copyView(srcView, view, true);
        ResponseMsg response = new ResponseMsg(true);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return response;
    }

    /**
     * 视图管理删除结果页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/delete")
    public ResponseMsg delete(HttpServletRequest request) throws Exception {
        View view = new View();
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        view.setCode(request.getParameter("view.code"));
        String version = request.getParameter("view.version");
        if (!StringUtils.isEmpty(version) && !"null".equals(version)) {
            view.setVersion(Integer.parseInt(version));
        }
        String moduleCode = view.getModuleCode();
        List<AdvQueryJson> advQueryJsons = new ArrayList<AdvQueryJson>();
        if(isProj){
            if(null !=request.getParameter("inheritType") && !request.getParameter("inheritType").equals("null") && !request.getParameter("inheritType").equals("") ){
                view.setInheritType(Integer.valueOf(request.getParameter("inheritType")));
            }
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        view = viewService.getView(view.getCode());
        String viewName = view.getName();
        String modelName = view.getAssModel().getModelName();
        String entityName = view.getEntity().getEntityName();
        String artifact = view.getEntity().getModule().getArtifact();
        if (isProj
                && (view.getType() == ViewType.EDIT
                || view.getType() == ViewType.VIEW || view.getType() == ViewType.EXTRA)
                && view.getInheritType() != null) {
            advQueryJsons = advQueryJsonService.findAdvQueryJsons(Restrictions
                    .eq("view", view));

        }
        ResponseMsg result = deleteChoise(view, true);
        if (isProj && result.isSuccess()) {
            ecDataSynchronizeService.synchronizeViewDataFromEC(view.getCode(),
                    DIRECTION_PROJ_TO_RUNTIME);
            if (view.getInheritType() != null) {
                view.setAdvQueryJson(advQueryJsons);
//                bapGenerateServiceV3.deleteProjViewFTL(view);
                viewService.updatePublishTimeToNull(view);
            }
            generateService.deleteProjViewHtml(viewName, modelName, entityName, artifact);
            msModuleServiceApi.updateLayoutJson(view.getModuleCode());
            //如果是全继承视图，删除后需要恢复下菜单url，改为去找产品期视图url
            if(null != view.getInheritType() &&view.getInheritType()==2 &&(view.getType() == ViewType.LIST || view.getType() == ViewType.EXTRA) && null != view.getUrl() && !"".equals(view.getUrl())){
                List<MenuInfo> menuInfos = menuInfoService.getMenuInfoByURL(view.getUrl());
                if(null !=menuInfos && menuInfos.size()>0){
                    for (MenuInfo menuInfo : menuInfos) {
                        if (menuInfo.getUrl().equals(view.getUrl())) {
                            menuInfo.setUrl(view.getUrl().replace("/proj/","/"));
                            menuInfoService.save(menuInfo);
                        }
                    }
                }
                actionViewService.refreshSingleViewAction(view, "ec");
            }
            //工程期新建的视图删除时隐藏菜单
            if(isProj && (view.getType() == ViewType.LIST || view.getType() == ViewType.EXTRA) && (view.getInheritType()==null)){
                List<MenuInfo> menuInfos = menuInfoService.getMenuInfoByURL(view.getUrl());
                for (MenuInfo menuInfo : menuInfos) {
                    if (!menuInfo.getIsHide() || !menuInfo.getAbsoluteHidden()) {
                        menuInfo.setIsHide(true);
                        menuInfo.setAbsoluteHidden(true);
                        menuInfoService.save(menuInfo);
                    }
                }
            }
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return result;
    }

    /**
     * 视图管理逻辑删除结果页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/ordinaryDelete")
    public ResponseMsg ordinaryDelete(View view) throws Exception {
        return deleteChoise(view, false);
    }

    private ResponseMsg deleteChoise(View view, boolean flag) throws JsonProcessingException {
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        if (null == view) {
            response.setSuccess(false);
            response.setExceptionMsg(InternationalResource.get("ec.view.notView"));
        } else {
            if (view.getInheritType() == null
                    && null != view.getUsedForWorkFlow()
                    && view.getUsedForWorkFlow()) {
                Set descSet = new HashSet<String>();
                descSet.add("不能删除该视图，需要先设置其它主列表视图");
                response.setSuccess(false);
                response.setExceptionMsg(JsonUtils.setToJson(descSet));
//                return mapper.writeValueAsString(response);
                // throw new BAPException(BAPException.Code.DELETE_MAIN_LIST);
                return response;
            }
            if (flag) {
                // add by yubo20171219
                String responseMsg = viewService.deleteViewPhysical(
                        view.getCode(), false);
                if (responseMsg != null && responseMsg.length() != 0
                        && (!("".equals(responseMsg)))) {
                    response.setSuccess(false);
                    response.setExceptionMsg(responseMsg);
//                    return mapper.writeValueAsString(response);
                    return response;
                }
                // 删除非移动视图同时删除移动视图
                if (!view.getMobile() && view.getInheritType() == null) {
                    String mobileResponseMsg = viewService.deleteViewPhysical(
                            view.getCode() + "__mobile__", false);
                    if (mobileResponseMsg != null
                            && mobileResponseMsg.length() != 0
                            && (!("".equals(mobileResponseMsg)))) {
                        response.setSuccess(false);
                        response.setExceptionMsg(mobileResponseMsg);
//                        return mapper.writeValueAsString(response);
                        return response;
                    }
                }
            } else {
                String responseMsg = viewService.deleteView(view);
                if (responseMsg != null && responseMsg.length() != 0
                        && (!("".equals(responseMsg)))) {
                    response.setSuccess(false);
                    response.setExceptionMsg(responseMsg);
//                    return mapper.writeValueAsString(response);
                    return response;
                }
            }
            response.setSuccess(true);
        }
//        return mapper.writeValueAsString(response);
        return response;
    }

    private static final String VIEW_CUSTOM = "CUSTOM";
    private static final String VIEW_EDIT = "EDIT";
    private static final String VIEW_EDIT_MOBILE = "EDIT-MOBILE";
    private static final String VIEW_LIST = "LIST";
    private static final String VIEW_VIEW = "VIEW";
    private static final String VIEW_TREE = "TREE";
    private static final String VIEW_REF_TREE = "REFTREE";
    // private static final String VIEW_SEARCH = "SEARCH";
    // private static final String VIEW_ADVSEARCH = "ADVSEARCH";
    private static final String VIEW_REFERENCE = "REFERENCE";
    private static final String VIEW_MNECODE = "MNECODE";
    private static final String VIEW_LAYOUT_A = "_LAYOUT";
    private static final String VIEW_LAYOUT_A2 = "_LAYOUT2";
    private static final String VIEW_DIGEST = "DIGEST";
    private static final String VIEW_EXTEA = "EXTRA";
    private static final String VIEW_EXTEA_MOBILE = "EXTRA-MOBILE";

    @RequestMapping({"/ec/view/config", "/ec/view/config-readonly"})
    public String config(ModelMap map, @RequestParam("view.code") String viewCode) throws Exception {
        HttpServletRequest request = getRequest();
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        long start = System.currentTimeMillis();
        View view = viewService.getView(viewCode, true);
        ExtraView ev = getExtraViewConfig(view);
        view.setExtraView(ev);
        log.info("config==查询视图和配置耗时：" + (System.currentTimeMillis() - start));
        map.addAttribute("view", view);
        map.addAttribute("ev",ev);
        map.addAttribute("isProj",isProj);
        map.addAttribute("TempProjView",getProjView(view, isProj));
        if (request.getRequestURI().equals("/ec/view/config-readonly")) {
            map.addAttribute("isReadOnlyMode", true);
        } else {
            map.addAttribute("isReadOnlyMode", false);
        }
        start = System.currentTimeMillis();
        String returnView = null;
        ViewType type = view.getType();
        if (ViewType.EDIT.equals(type) || ViewType.VIEW.equals(type) || ViewType.EXTRA.equals(type)) {
            if (view.getMobile() != null && view.getMobile()) {
                editMobileViewConfig(view, map);
                returnView = "view/config-edit-mobile";
            } else {
                extraViewConfig(view, map);
                returnView = "view/config-extra";
            }
        } else if (ViewType.LIST.equals(type) || ViewType.REFERENCE.equals(type)) {
            if (view.getMobile() != null && view.getMobile()) {
                listMobileViewConfig(view, map);
                returnView = "view/config-extraMobileList";
            } else if(view.getShowType() == ShowType.LAYOUT2) {
                layoutConfig(view, map);
                returnView = "view/config-newlist-layout2";
            } else {
                listViewConfig(view, map);
                returnView = "view/config-newlist";
            }
        } else if (ViewType.MNECODE.equals(type)) {
            mnecodeViewConfig(view, map);
            returnView = "view/config-mnecode";
        }else if (ViewType.DIGEST.equals(type)) {
            digestViewConfig(view, map);
            returnView = "view/config-digest";
        } else if (ViewType.REFTREE.equals(type) || ViewType.TREE.equals(type)) {
            treeViewConfig(view, map);
            returnView = "view/config-tree";
        }
        log.info("config==map耗时：" + (System.currentTimeMillis() - start));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return returnView;
    }

    private View getProjView(View view, Boolean isProj) {
        if (!isProj && !(view.getMobile() != null && view.getMobile())) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
            View tempProjView = viewService
                    .getViewByHql(
                            "from View where valid=true and projFlag = true and code = ?0 ",
                            view.getCode());
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
            return tempProjView;
        }
        return null;
    }

    private ExtraView getExtraViewConfig(View view) {
        long start = System.currentTimeMillis();
        ExtraView ev = view.getExtraView();
        if (null != ev) {
            if (view.getType() != ViewType.MNECODE
                    && view.getShowType() != ShowType.LAYOUT) {
                String evConfig = viewService.getExtraViewFullConfig(view);
                ev.setConfig(evConfig);
                log.debug("===" + evConfig);
                Map configMap = (Map) SerializeUitls.deserialize(evConfig);
                ev.setConfigMap(configMap);
            }
        } else {
            ev = viewService.defaultExtraView(view);
        }
        log.info("config==查询ExtraViewConfig耗时：" + (System.currentTimeMillis() - start));
        return ev;
    }

    private void editMobileViewConfig(View view, ModelMap map) {
        Map<String, Property> propertyMap = new HashMap<String, Property>();
        if (!StringUtils.isEmpty(view.getExtraView().getConfig())) {
            propertyMap = viewService.getPropertyMap(view.getExtraView().getConfig());
        }
        map.addAttribute("propertyMap", propertyMap);
        Model model = modelService.getModel(view.getAssModel().getCode());
        List<Property> properties = modelService.findProperties(model);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
        map.addAttribute("assos", getAssociatedInfoList(view, origAssociatedInfos));
        map.addAttribute("subs", getSubEntitiesAndProperties(model, properties, origAssociatedInfos, "list"));
        map.addAttribute("multSelectInfo", getMultSelectInfo(view));
        map.addAttribute("fieldTypes", fieldTypes);
        map.addAttribute("viewColumnTypes", viewColumnTypes);
        map.addAttribute("viewFormats", viewFormats);
    }

    private void listMobileViewConfig(View view, ModelMap map) {
        Model model = modelService.getModel(view.getAssModel().getCode());
        List<Property> properties = modelService.findProperties(model);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
        map.addAttribute("subs", getSubEntitiesAndProperties(model, properties, origAssociatedInfos, "list"));
        map.addAttribute("multSelectInfo", getMultSelectInfo(view));
        List<View> viewList = viewService.findViews(view.getEntity(), 1, 3, 0, ViewType.EDIT,
                ViewType.EXTRA);
        map.addAttribute("viewList", viewList);
        List<Model> assModels = new ArrayList<Model>();
        List<AssociatedInfo> list = modelService.findAssociatedInfos(view
                .getAssModel());
        if (!list.isEmpty()) {
            for (AssociatedInfo ass : list) {
                if (ass.getType() == Property.ONE_TO_MANY
                        || (ass.getType() == Property.MANY_TO_ONE
                        && ass.getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(ass.getOriginalProperty()
                                .getModel().getCode()) && !ass
                        .getTargetProperty().getModel().getEntity()
                        .getWorkflowEnabled())) {
                    assModels.add(ass.getTargetProperty().getModel());
                }
            }
        }
        map.addAttribute("assos", getAssociatedInfoList(view, origAssociatedInfos));
        List<Model> models = modelService.findModels(view.getEntity());
        map.addAttribute("models", models);
        map.addAttribute("multSelectInfo", getMultSelectInfo(view));
        map.addAttribute("assModels", assModels);
        map.addAttribute("fieldTypes", fieldTypes);
        map.addAttribute("viewTypes", viewTypes);
        map.addAttribute("viewColumnTypes", viewColumnTypes);
        map.addAttribute("viewFormats", viewFormats);
    }

    private void layoutConfig(View view, ModelMap map) {
        Layout layout = layoutService.get(view.getLayoutCode());
        layout.setConfigMap((Map) SerializeUitls.deserialize(layout
                .getContent()));
        map.addAttribute("layout", layout);
    }

    private void treeViewConfig(View view, ModelMap map) {
        List<View> viewList = viewService.findViews(view.getEntity(), 1, 3, 0, ViewType.EDIT);
        map.addAttribute("viewList", viewList);

        Map<String, Object> subs = new HashMap<String, Object>();
        subs.put("properties",
                modelService.findPropertiesByModel(view.getAssModel()));
        map.addAttribute("subs", subs);
    }

    private void digestViewConfig(View view, ModelMap map) {
        Model model = modelService.getModel(view.getAssModel().getCode());
        List<Property> properties = modelService.findProperties(model);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
        map.addAttribute("subs", getSubEntitiesAndProperties(model, properties, origAssociatedInfos, "list"));
        Map<String, Property> propertyMap = new HashMap<String, Property>();
        if (!StringUtils.isEmpty(view.getExtraView().getConfig())) {
            propertyMap = viewService.getPropertyMap(view.getExtraView().getConfig());
        }
        map.addAttribute("propertyMap", propertyMap);
    }

    private void mnecodeViewConfig(View view, ModelMap map) {
        Map<String, List<Property>> assMap = new HashMap<String, List<Property>>();
        Map<String, String> relativeMap = new HashMap<String, String>();
        List<AssociatedInfo> list = modelService.findAssociatedInfos(
                view.getAssModel(), 1, 2);
        List<Property> propertyList = new ArrayList<Property>();
        if (!list.isEmpty()) {
            for (AssociatedInfo ass : list) {
                for (Property p : view.getAssModel().getProperties()) {
                    if (p.getAssociatedProperty() != null) {
                        if (ass.getOriginalProperty().getCode().equals(p.getCode())) {
                            Model model = null;
                            List<AssociatedInfo> assos = modelService.findAssociatedInfos(modelService.getProperty(p.getCode()));
                            if (assos != null) {
                                for (AssociatedInfo asso : assos) {
                                    if ((asso.getType() == AssociatedInfo.ONE_TO_ONE || asso.getType() == AssociatedInfo.MANY_TO_ONE) && asso.getOriginalProperty().getCode().equals(p.getCode())) {
                                        model = asso.getTargetProperty().getModel();
                                        relativeMap.put(p.getCode(), model.getTableName()
                                                + ","
                                                + asso.getTargetProperty()
                                                .getColumnName()
                                                + ","
                                                + asso.getOriginalProperty()
                                                .getModel()
                                                .getTableName()
                                                + ","
                                                + asso.getOriginalProperty()
                                                .getColumnName());
                                        break;
                                    }
                                }
                            }
                            propertyList = modelService
                                    .findProperties(model);
                            assMap.put(p.getCode(), propertyList);

                        }
                    }
                }
            }
        }
        map.addAttribute("assMap", assMap);
        map.addAttribute("relativeMap", relativeMap);
    }

    private void listViewConfig(View view, ModelMap map) {
        List<View> viewList = viewService.findViews(view.getEntity(), 1, 3, 0, ViewType.EDIT,
                ViewType.EXTRA);
        map.addAttribute("viewList", viewList);
        Model model = modelService.getModel(view.getAssModel().getCode());
        List<Property> properties = modelService.findProperties(model);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
        map.addAttribute("subs", getSubEntitiesAndProperties(model, properties, origAssociatedInfos, "list"));
        List<DataGroup> dgList = viewService.findDataGroups(view);
        if (!dgList.isEmpty()) {
            for (DataGroup dg : dgList) {
                Set<DataClassific> dcSet = new HashSet<DataClassific>();
                List<DataClassific> dcList = viewService
                        .findDataClassifics(dg);
                if (!dcList.isEmpty()) {
                    for (DataClassific dc : dcList) {
                        dcSet.add(dc);
                    }
                }
                dg.setDataClassifics(dcSet);
            }
        }
        map.addAttribute("dgList", dgList);
        List<View> assViews = null;
        if (view.getType() == ViewType.REFERENCE) {
            assViews = viewService.findViews( view.getEntity(), 1, 4, 0);
            for (Iterator<View> it = assViews.iterator(); it.hasNext(); ) {
                View v = it.next();
                if (v.getType().equals(ViewType.LIST) && !v.getIsShadow()
                        && (v.getShowType().equals(ShowType.SINGLE))) {
                    continue;
                } else {
                    it.remove();
                }
            }
        }
        map.addAttribute("assViews", assViews);
        List<Model> assModels = new ArrayList<Model>();
        List<AssociatedInfo> list = modelService.findAssociatedInfos(view
                .getAssModel());
        if (!list.isEmpty()) {
            for (AssociatedInfo ass : list) {
                if (ass.getType() == Property.ONE_TO_MANY
                        || (ass.getType() == Property.MANY_TO_ONE
                        && ass.getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(ass.getOriginalProperty()
                                .getModel().getCode()) && !ass
                        .getTargetProperty().getModel().getEntity()
                        .getWorkflowEnabled())) {
                    assModels.add(ass.getTargetProperty().getModel());
                }
            }
        }
        map.addAttribute("assModels", assModels);
        Map<String, Property> propertyMap = new HashMap<String, Property>();
        if (view.getExtraView() != null && !StringUtils.isEmpty(view.getExtraView().getConfig())) {
            propertyMap = viewService.getPropertyMap(view.getExtraView().getConfig());
        }
        map.addAttribute("propertyMap", propertyMap);
        map.addAttribute("viewColumnTypes", viewColumnTypes);
        map.addAttribute("viewFormats", viewFormats);
        map.addAttribute("fastfieldtypes", fastfieldtypes);
    }

    private void extraViewConfig(View view, ModelMap map) {
        Model model = modelService.getModel(view.getAssModel().getCode());
        List<Property> properties = modelService.findProperties(model);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(model);
        map.addAttribute("subs", getSubEntitiesAndProperties(model, properties, origAssociatedInfos, "list"));
        map.addAttribute("assos", getAssociatedInfoList(view, origAssociatedInfos));
        List<Model> models = modelService.findModels(view.getEntity());
        map.addAttribute("models", models);
        map.addAttribute("multSelectInfo", getMultSelectInfo(view));
        List<SystemCode> layerTypes = systemCodeService
                .getSystemCodeByEntity(layerSystemEntityCode);
        map.addAttribute("layerTypes", layerTypes);
        map.addAttribute("fieldTypes", fieldTypes);
        map.addAttribute("viewColumnTypes", viewColumnTypes);
        map.addAttribute("viewFormats", viewFormats);
        map.addAttribute("workFlowElements", workFlowElements);
        Map<String, Property> orgPropertyMap = new HashMap<String, Property>();
        Map<String, Model> targetModelMap = new HashMap<String, Model>();
        List<DataGrid> dataGrid = dataGridService.getDataGridByView(view,
                false);
        if (null != dataGrid) {
            for (DataGrid dg : dataGrid) {
                if (null != dg.getOrgProperty()) {
                    orgPropertyMap.put(dg.getCode(), dg.getOrgProperty());
                } else {
                    targetModelMap.put(dg.getCode(), dg.getTargetModel());
                }
            }
        }
        map.addAttribute("orgPropertyMap", orgPropertyMap);
        map.addAttribute("targetModelMap", targetModelMap);
    }


    private List<AssociatedInfo> getAssociatedInfoList(View view,  List<AssociatedInfo> list) {
        List<Model> assModels = new ArrayList<Model>();
        if (!list.isEmpty()) {
            for (AssociatedInfo ass : list) {
                if (ass.getType() == Property.ONE_TO_MANY
                        || (ass.getType() == Property.MANY_TO_ONE
                        && ass.getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(ass.getOriginalProperty()
                                .getModel().getCode()) && !ass
                        .getTargetProperty().getModel().getEntity()
                        .getWorkflowEnabled())) {
                    assModels.add(ass.getTargetProperty().getModel());
                }
            }
        }
        List<AssociatedInfo> assos = null;
        if (assModels != null && !assModels.isEmpty()) {
            assos = modelService.findAssociatedInfos(assModels,
                    AssociatedInfo.MANY_TO_ONE);
            if (assos != null) {
                for (Iterator<AssociatedInfo> it = assos.iterator(); it
                        .hasNext(); ) {
                    AssociatedInfo asso = it.next();
                    if (asso.getType() != AssociatedInfo.MANY_TO_ONE
                            || !asso.getTargetProperty().getModel()
                            .getCode()
                            .equals(view.getAssModel().getCode())) {
                        it.remove();
                    } else if (!view.getEntity().getCode().equals(asso
                            .getOriginalProperty().getEntityCode())) { // fix
                        // qc-5725
                        // 关联pt的作用范围限制在本实体内
                        it.remove();
                    }
                }
            }
        }
        return assos;
    }

    private Map<String, Object> getMultSelectInfo(View view) {
        Map<String, Object> indirectMap = new HashMap<String, Object>();
        List<AssociatedInfo> assos = modelService.findAssociatedInfos(
                view.getAssModel(), AssociatedInfo.ONE_TO_MANY);
        List<AssociatedInfo> directAssos = new ArrayList<AssociatedInfo>();
        List<Model> models = modelService.findModels(view.getEntity());
        for (AssociatedInfo item : assos) {
            if (models.contains(item.getTargetProperty().getModel())
                    && item.getIsMainAssociated() != null
                    && item.getIsMainAssociated()) {
                directAssos.add(item);
            }
        }
        Iterator<AssociatedInfo> it = directAssos.iterator();
        while (it.hasNext()) {
            AssociatedInfo item = it.next();
            if ("id".equalsIgnoreCase(item.getTargetProperty().getName())) {
                it.remove();
            } else {
                assos = modelService.findAssociatedInfoNotIncludeBackAsso(
                        item.getTargetProperty().getModel(),
                        AssociatedInfo.ONE_TO_ONE,
                        AssociatedInfo.MANY_TO_ONE);
                if (assos == null || assos.isEmpty()) {
                    it.remove();
                } else {
                    Iterator<AssociatedInfo> ait = assos.iterator();
                    while (ait.hasNext()) {
                        AssociatedInfo ass = ait.next();
                        if (ass.getIsMainAssociated() != null
                                && ass.getIsMainAssociated()) {
                            ait.remove();
                        }
                    }
                    indirectMap.put("-"
                                    + item.getOriginalProperty().getCode() + "-"
                                    + item.getTargetProperty().getCode() + "-",
                            assos);
                }
            }
        }
        Map<String, Object> multSelectInfo = new HashMap<String, Object>();
        multSelectInfo.put("directAssos", directAssos);
        String excludes = "*.companyStaffs,*.departmentWorks,*.positionWorks,*.models,*.entities,*.views,*.properties,*.class,*.createTime,*.createStaff,*.createStaffId,*.modifyStaff,*.modifyTime,*.modifyStaffId,*.deleteStaff,*.deleteStaffId,*.deleteTime,*.entity";
        multSelectInfo.put("directAssosJson",
                ConditionUtil.serialize(directAssos, null, excludes));
        multSelectInfo.put("indirectAssosJson",
                ConditionUtil.serialize(indirectMap, null, excludes));
        return multSelectInfo;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/changeLayout")
    public void changeLayoutName(String viewCode) throws Exception {
        HttpServletRequest request = getRequest();
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        String oldLayoutName = request.getParameter("oldLayoutName").toString();
        String newLayoutName = request.getParameter("newLayoutName").toString();
        if (null != oldLayoutName && !"".equals(oldLayoutName)
                && null != newLayoutName && !"".equals(newLayoutName)
                && null != viewCode && !"".equals(viewCode)) {
            viewService
                    .changeLayoutName(viewCode, oldLayoutName, newLayoutName);
        }
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
    }

    @RequestMapping({"/ec/view/dataclassific_config", "/ec/view/dataclassific_config_readonly"})
    public String dataClassificConfig(ModelMap map, HttpServletRequest request) throws Exception {
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        String viewCode = request.getParameter("view.code");
        View view = viewService.getView(viewCode);
        Model targetModel = modelService.getModel(view.getAssModel().getCode());
        Boolean isReadOnlyMode = false;
        if (getRequest().getRequestURI().equals(
                "/ec/view/dataclassific_config_readonly")) {
            isReadOnlyMode = true;
        }
        view = viewService.getView(view.getCode());
        map.addAttribute("view", view);
        map.addAttribute("targetModel", targetModel);
        map.addAttribute("isReadOnlyMode", isReadOnlyMode);
        map.addAttribute("isProj",isProj);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/config-dataclassific";
    }

    /**
     * 增强型视图快速查询配置
     *
     * @return
     * @throws Exception
     */
    @RequestMapping({"/ec/view/extraFastQuery_config", "/ec/view/extraFastQuery_config_readonly"})
    public String extraFastQueryConfig(ModelMap map, @RequestParam("view.code") String viewCode, @RequestParam("targetModel.code") String targetModelCode, String layoutName) throws Exception {
        Boolean isReadOnlyMode = false;
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (getRequest().getRequestURI().equals(
                "/ec/view/extraFastQuery_config_readonly")) {
            isReadOnlyMode = true;

        }
        View view = viewService.getView(viewCode, true);
        Model targetModel=modelService.getModel(targetModelCode);
        List<Property> properties = modelService.findProperties(targetModel);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(targetModel);
        Map<String, Object> subs = getSubEntitiesAndProperties(targetModel, properties, origAssociatedInfos, "list");
        FastQueryJson fqj = viewService.getFastQueryJsonByViewCodeAndLayoutName(view,
                layoutName);
        Map<String, Property> propertyMap = null;
        if (fqj != null && fqj.getQueryConfig() != null
                && !fqj.getQueryConfig().equals("")) {
            Map configMap = (Map) SerializeUitls.deserialize(fqj
                    .getQueryConfig());
            fqj.setQueryConfigMap(configMap);
            propertyMap = viewService.getPropertyMap(fqj.getQueryConfig());
        } else {
            if (fqj == null) {
                fqj = new FastQueryJson();
                fqj.setCode(view.getCode() + System.currentTimeMillis());
            }
            fqj.setView(view);
        }
        if (null == fqj.getTargetModel()) {
            fqj.setTargetModel(targetModel);
        }
        map.addAttribute("view", view);
        map.addAttribute("subs", subs);
        map.addAttribute("fqj", fqj);
        map.addAttribute("propertyMap", propertyMap);
        map.addAttribute("isReadOnlyMode", isReadOnlyMode);
        map.addAttribute("targetModel", targetModel);
        map.addAttribute("isProj", isProj);
        map.addAttribute("layoutName", layoutName);
        map.addAttribute("isProj",isProj);
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return "view/config-extraFastQuery";
    }

    /**
     * 增强型视图 高级查询配置
     *
     * @return
     * @throws Exception
     */
    @RequestMapping({"/ec/view/extraAdvQuery_config", "/ec/view/extraAdvQuery_config_readonly"})
    public String extraAdvQueryConfig(ModelMap map, @RequestParam("view.code") String viewCode, @RequestParam("targetModel.code") String targetModelCode, String layoutName) throws Exception {
        Boolean isReadOnlyMode = false;
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (getRequest().getRequestURI().equals(
                "/ec/view/extraAdvQuery_config_readonly")) {
            isReadOnlyMode = true;
        }
        View view = viewService.getView(viewCode, true);
        Model targetModel=modelService.getModel(targetModelCode);
        List<Property> properties = modelService.findProperties(targetModel);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(targetModel);
        Map<String, Object> subs = getSubEntitiesAndProperties(targetModel, properties, origAssociatedInfos, "list");
        AdvQueryJson aqj = viewService.getAdvQueryJsonByViewCodeAndLayoutName(view,
                layoutName);
        Map<String, Property> propertyMap = null;
        if (aqj != null && aqj.getQueryConfig() != null
                && !aqj.getQueryConfig().equals("")) {
            Map configMap = (Map) SerializeUitls.deserialize(aqj
                    .getQueryConfig());
            aqj.setQueryConfigMap(configMap);
            propertyMap = viewService.getPropertyMap(aqj.getQueryConfig());
        } else {
            if (aqj == null) {
                aqj = new AdvQueryJson();
                Long currentTimes = System.currentTimeMillis();
                aqj.setCode(view.getCode() + currentTimes);
                aqj.setName(view.getName() + currentTimes);
            }
            aqj.setView(view);
        }
        if (null == aqj.getTargetModel()) {
            aqj.setTargetModel(targetModel);
        }
        map.addAttribute("view", view);
        map.addAttribute("subs", subs);
        map.addAttribute("aqj", aqj);
        map.addAttribute("isReadOnlyMode", isReadOnlyMode);
        map.addAttribute("targetModel", targetModel);
        map.addAttribute("isProj", isProj);
        map.addAttribute("layoutName", layoutName);
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return "view/config-extraAdvQuery";
    }

    @RequestMapping(value = "/ec/view/dataModelTree")
    public String dataModelTree(ModelMap map, View view) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        view = viewService.getView(view.getCode());
        String viewName = view.getAssModel().getModelName();
        Model targetModel=modelService.getModel(view.getAssModel().getCode());
        List<Property> properties = modelService.findProperties(targetModel);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(targetModel);
        Map<String, Object> subs = getSubEntitiesAndProperties(targetModel, properties, origAssociatedInfos, "list");
        map.addAttribute("view", view);
        map.addAttribute("viewName", viewName);
        map.addAttribute("subs", subs);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/dataModelTree";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/data_grouplist")
    public List<DataGroupVO> datagrouplist(String layoutName, HttpServletRequest request) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        String viewCode = request.getParameter("view.code");
        View view = viewService.getView(viewCode);

        String modelCode = request.getParameter("targetModel.code");
        Model targetModel = modelService.getModel(modelCode);
        List<DataGroup> dataGroupList = null;
        if (layoutName != null && !layoutName.equals("")) {
            if (null != targetModel) {
                dataGroupList = viewService.findDataGroups(view, layoutName,
                        targetModel.getCode());
            }
        } else {
            dataGroupList = viewService.findDataGroups(view);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
       return dataGroupWrapper.e2vList(dataGroupList);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/data_classificlist")
    public Page<DataClassific> dataclassificlist(HttpServletRequest request, @RequestParam(required = false, defaultValue = "1") Integer pageNo, @RequestParam(required = false, defaultValue = "20") Integer pageSize) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        final String dataGroupCode = request.getParameter("dataGroup.code");
        DataGroup dataGroup = new DataGroup();
        dataGroup.setCode(dataGroupCode);

        if (pageSize > 500) {
            pageSize = 500;
        }

        if (null != dataGroup.getCode()) {
            Page<DataClassific> dataClassificList = viewService.findDataClassifics(new Page<DataClassific>(pageNo, pageSize), dataGroup);
            List<DataClassific> dataClassifics=dataClassificList.getResult();
            for (DataClassific dc:dataClassifics) {
                dc.setDisplayName(internationalService.getI18nValue(dc.getDisplayName()));
            }
            return dataClassificList;
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return new Page<DataClassific>();
    }

    /**
     * 进入数据分组编辑页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/view/datagroupedit")
    public String dataGroupEdit(ModelMap map, HttpServletRequest request, DataGroup dataGroup) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        String viewCode = request.getParameter("view.code");
        View view = viewService.getView(viewCode);
        view = viewService.getView(view.getCode());
        if (null != dataGroup && null != dataGroup.getCode()) {
            dataGroup = viewService.getDataGroup(dataGroup.getCode());
        }
        map.addAttribute("view", view);
        map.addAttribute("dataGroup", dataGroup);
        map.addAttribute("isProj",isProj);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/config-datagroupedit";
    }

    /**
     * 进入数据分类编辑页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/view/dataclassificedit")
    public String dataClassificEdit(ModelMap map, HttpServletRequest request) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if(isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        String viewCode = request.getParameter("view.code");
        View view = viewService.getView(viewCode);
        String modelCode = request.getParameter("targetModel.code");
        Model targetModel = modelService.getModel(modelCode);
        String dataGroupCode = request.getParameter("dataGroup.code");
        String dataClassificCode = request.getParameter("dataClassific.code");
        DataClassific dataClassific = viewService.getDataClassific(dataClassificCode);

        if (null != dataClassific && null != dataClassific.getCode()) {
            dataClassific = viewService.getDataClassific(dataClassific
                    .getCode());
        }
        if (null != targetModel && null != targetModel.getCode()) {
            targetModel = modelService.getModel(targetModel.getCode());
        }
        map.addAttribute("view", view);
        map.addAttribute("dataClassific", dataClassific);
        map.addAttribute("targetModel", targetModel);
        map.addAttribute("isProj", isProj);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/config-dataclassificedit";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/datagroupsave")
    public DataGroupVO dataGroupSave(HttpServletRequest request, String layoutName, Model targetModel) throws Exception {
        DataGroup dataGroup = DtoUtils.getDataGroupVO(request);
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(null!=isProj && isProj){
            dataGroup.setProjFlag(true);
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (dataGroup != null) {
            if (layoutName != null && !layoutName.equals("")) {
                dataGroup.setLayoutName(layoutName);
            }
            if (null != targetModel) {
                dataGroup.setTargetModel(targetModel);
            }
            viewService.saveDataGroup(dataGroup);
            dataGroup = viewService.getDataGroup(dataGroup.getCode());
            return dataGroupWrapper.e2v(dataGroup);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return dataGroupWrapper.e2v(dataGroup);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/dataclassificsave")
    public String dataClassificSave(HttpServletRequest request) throws Exception {
        DataClassific dataClassific = DtoUtils.getDataClassificVO(request);
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(null!=isProj && isProj){
            dataClassific.setProjFlag(true);
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        viewService.saveDataClassific(dataClassific);
        ResponseMsg response = new ResponseMsg(true);
        response.setData(dataClassific.getCode());
        ObjectMapper mapper = new ObjectMapper();
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    /**
     * 删除数据分组页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/datagroupdelete")
    public ResponseMsg dataGroupDelete(HttpServletRequest request) throws Exception {
        DataClassific dataClassific = DtoUtils.getDataClassificVO(request);
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(null!=isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        DataGroup dataGroup = new DataGroup();
        String dataGroupCode = request.getParameter("dataGroup.code");
        if (!StringUtils.isEmpty(request.getParameter("dataGroup.version"))) {
            int version = Integer.parseInt(request.getParameter("dataGroup.version"));
            dataGroup.setVersion(version);
        }
        dataGroup.setCode(dataGroupCode);

        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        if (null == dataGroup) {
            response.setSuccess(false);
            response.setExceptionMsg(InternationalResource.get(
                    "ec.entity.viewaction.nodatagroup"));
        } else {
            viewService.deleteDataGroup(dataGroup);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return response;
    }

    /**
     * 删除数据分类页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/dataclassificdelete")
    public ResponseMsg dataClassificDelete(HttpServletRequest request) throws Exception {
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(null!=isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        String dataClassificCode = request.getParameter("dataClassific.code");
        DataClassific dataClassific = viewService.getDataClassific(dataClassificCode);
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        if (null == dataClassific) {
            response.setSuccess(false);
            response.setExceptionMsg(InternationalResource.get(
                    "ec.entity.viewaction.nodataclass"));
        } else {
            viewService.deleteDataClassific(dataClassific);
        }
//        return mapper.writeValueAsString(response);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return response;
    }

    @RequestMapping(value = "/ec/view/layout-property2")
    public String layoutProperty2(ModelMap map, @RequestParam("view.code") String viewCode, @RequestParam("part") String part) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View view = viewService.getView(viewCode, true);
        List<View> viewList = viewService.findViews(view.getEntity().getModule());
        List<Model> models = modelService.findModels(view.getEntity().getModule());
        Model deptModel = modelService
                .getModel("sysbase_1.0_department_base_department");
        Model posiModel = modelService
                .getModel("sysbase_1.0_position_base_position");
        models.add(deptModel);
        models.add(posiModel);
        List<ModuleRelation> relations = moduleService.getRelations(view
                .getEntity().getModule());
        for (ModuleRelation relation : relations) {
            viewList.addAll(viewService.findViews(relation.getTarget()));
        }
        List<ModuleReference> references = moduleReferenceService
                .getReferences(view.getEntity().getModule());
        for (ModuleReference reference : references) {
            viewList.addAll(viewService.findViews(reference.getTarget()));
        }
        // 基础模块视图
        Module foundation = new Module();
        foundation.setCode("sysbase_1.0");
        viewList.addAll(viewService.findViews(foundation));
        map.addAttribute("view", view);
        map.addAttribute("viewList", viewList);
        map.addAttribute("models", models);
        map.addAttribute("part", part);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/layout-property2";
    }

    @RequestMapping(value = "/ec/view/layout-property")
    public String layoutProperty(ModelMap map, @RequestParam("view.code") String viewCode, String part) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View view = viewService.getView(viewCode, true);
        List<View> viewList = viewService.findViews(view.getEntity().getModule());
        List<Model> models = modelService.findModels(view.getEntity().getModule());
        Model deptModel = modelService
                .getModel("sysbase_1.0_department_base_department");
        Model posiModel = modelService
                .getModel("sysbase_1.0_position_base_position");
        models.add(deptModel);
        models.add(posiModel);
        map.addAttribute("viewList", viewList);
        map.addAttribute("models", models);
        map.addAttribute("part", part);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/layout-property";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/findPropertiesByModel")
    public List<PropertyVO> findPropertiesByModel(@RequestParam("model.code")String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<Property> tagPropertyList = null;
        if (null != modelCode) {
            Model model = new Model();
            model.setCode(modelCode);
            tagPropertyList = modelService.findProperties(model);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return propertyWrapper.e2vList(tagPropertyList);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/findTreeModelsByModel")
    public List<AssociatedInfo> findTreeModelsByModel(String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<AssociatedInfo> assTreeModels = new ArrayList<AssociatedInfo>();
        Model model = modelService.getModel(modelCode);
        List<AssociatedInfo> assos = modelService.findAssociatedInfos(model,
                AssociatedInfo.ONE_TO_ONE, AssociatedInfo.MANY_TO_ONE);
        Set<String> isExistAss = new HashSet<String>();
        for (AssociatedInfo asso : assos) {
            model = asso.getTargetProperty().getModel();
            if (model.getDataType() == 2) {
                if (!isExistAss.contains(asso.getOriginalProperty().getName()
                        + "." + asso.getTargetProperty().getName())) {
                    isExistAss.add(asso.getOriginalProperty().getName() + "."
                            + asso.getTargetProperty().getName());
                    // model.setTreeAssCode(asso.getOriginalProperty().getName()+"."+asso.getTargetProperty().getName());
                    assTreeModels.add(asso);
                }
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return assTreeModels;
    }

    @ResponseBody
    @RequestMapping("/ec/view/findTreeModelsMapByModel")
    public Map<String, List> findTreeModelsMapByModel(String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        Map<String, List> assTreeMap = new HashMap<String, List>();
        List<Model> modelSelf = new ArrayList<Model>();
        List<AssociatedInfo> assTreeModels = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> notTreeModels = new ArrayList<AssociatedInfo>();
        Model model = modelService.getModel(modelCode);
        modelSelf.add(model);
        List<AssociatedInfo> assos = modelService
                .findAssociatedInfoNotIncludeBackAsso(model,
                        AssociatedInfo.ONE_TO_ONE, AssociatedInfo.MANY_TO_ONE);
        Set<String> isExistAss = new HashSet<String>();
        for (AssociatedInfo asso : assos) {
            model = asso.getTargetProperty().getModel();
            if (model.getDataType() == 2) {
                if (!isExistAss.contains(asso.getOriginalProperty().getName()
                        + "." + asso.getTargetProperty().getName())) {
                    isExistAss.add(asso.getOriginalProperty().getName() + "."
                            + asso.getTargetProperty().getName());
                    assTreeModels.add(asso);
                }
            } else {
                notTreeModels.add(asso);
            }
        }
        assTreeMap.put("modelSelf", modelSelf);
        assTreeMap.put("treeModels", assTreeModels);
        assTreeMap.put("notTreeModels", notTreeModels);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return assTreeMap;
    }

    @RequestMapping("/ec/view/showTreeModelsMapByModel")
    public String findTreeModelsMapByModel(ModelMap map, String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        Map<String, List> assTreeMap = new HashMap<String, List>();
        List<Model> modelSelf = new ArrayList<Model>();
        List<AssociatedInfo> assTreeModels = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> notTreeModels = new ArrayList<AssociatedInfo>();
        Model model = modelService.getModel(modelCode);
        modelSelf.add(model);
        List<AssociatedInfo> assos = modelService
                .findAssociatedInfoNotIncludeBackAsso(model,
                        AssociatedInfo.ONE_TO_ONE, AssociatedInfo.MANY_TO_ONE);
        Set<String> isExistAss = new HashSet<String>();
        for (AssociatedInfo asso : assos) {
            model = asso.getTargetProperty().getModel();
            if (model.getDataType() == 2) {
                if (!isExistAss.contains(asso.getOriginalProperty().getName()
                        + "." + asso.getTargetProperty().getName())) {
                    isExistAss.add(asso.getOriginalProperty().getName() + "."
                            + asso.getTargetProperty().getName());
                    assTreeModels.add(asso);
                }
            } else {
                notTreeModels.add(asso);
            }
        }
        assTreeMap.put("modelSelf", modelSelf);
        assTreeMap.put("treeModels", assTreeModels);
        assTreeMap.put("notTreeModels", notTreeModels);
        map.addAttribute("assTreeMap", assTreeMap);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/ass-model-list";
    }


    /**
     * 获取一对一或者多对一的关联对象的属性
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/AssociatedProperty")
    public List<PropertyVO> AssociatedProperty(String propertyCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        Model model = null;
    	List<AssociatedInfo> assos = modelService
                .findAssociatedInfos(modelService.getProperty(propertyCode));
        if (CollectionUtils.isNotEmpty(assos)) {
            for (AssociatedInfo asso : assos) {
                if ((asso.getType() == AssociatedInfo.ONE_TO_ONE || asso
                        .getType() == AssociatedInfo.MANY_TO_ONE)
                        && asso.getOriginalProperty().getCode()
                        .equals(propertyCode)) {
                    model = asso.getTargetProperty().getModel();
                    break;
                }
            }
        } else {
        	CustomPropertyModelMapping customPropertyModelMapping = modelService.getAssociatedCustomPropertyModelMapping(propertyCode);
        	if (null != customPropertyModelMapping) {
        		model = customPropertyModelMapping.getAssociatedProperty().getModel();
        	}
        }
        List<Property> tagPropertyList = modelService.findProperties(model);
        Iterator<Property> iterator = tagPropertyList.iterator();
        while (iterator.hasNext()) {
            Property p = iterator.next();
            if (p.getIsCustom()) {
                iterator.remove();
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return propertyWrapper.e2vList(tagPropertyList);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/getMneCode")
    public ExtraViewVO getMneCode(@RequestParam("view.code") String viewCode,@RequestParam(value = "isProj", required = false) Boolean isProj) {
        if(null != isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View view = viewService.getView(viewCode, true);
        ExtraView ev = view.getExtraView();
        if (null != ev) {
            /* 已经配置过 */

            String evConfig = viewService.getExtraViewFullConfig(view);
            Map configMap = (Map) SerializeUitls.deserialize(evConfig);
            ev.setConfigMap(configMap);
        } else {
            return null;
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return extraViewWrapper.e2v(ev);
    }

    /**
     * 获取entity下的参照视列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/referenceViews")
    public List<ViewVO> referenceViews(Entity entity, Long modelId, String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<View> viewList = null;
        if (entity != null && entity.getCode() != null) {
            entity = entityService.getEntity(entity.getCode());
            viewList = viewService.findViews(entity, ViewType.REFERENCE);
        } else if (modelId != null && modelId > 0) {
            viewList = viewService.findViewsByAssModelCode(modelCode,
                    ViewType.REFERENCE);
        } else if (modelCode != null && modelCode.length() > 0) {
            viewList = viewService.findViewsByAssModelCode(modelService
                    .getModel(modelCode).getCode(), ViewType.REFERENCE);
        }
        if (null != viewList && !viewList.isEmpty()) {
            for (Iterator<View> it = viewList.iterator(); it.hasNext(); ) {
                View v = it.next();
                if (v.getShowType() == ShowType.PART) {
                    // viewList.remove(v);
                    it.remove();
                }
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return viewWrapper.e2vList(viewList);
    }

    /**
     * 获取字段的本身模型和上级关联模型
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/columnModels")
    public List<ModelVO> columnModels(String propertyCode, String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<Model> models = new ArrayList<Model>();
        models.add(modelService.getProperty(propertyCode).getModel());
        if (null != modelCode && !"".equals(modelCode)) {
            models.add(modelService.getModel(modelCode));
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return modelWrapper.e2vList(models);
    }

    /**
     * 获取model下的查看视图列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/viewView")
    public List<ViewVO> viewView(String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<View> viewList = viewService.findViewsByAssModelCode(
                modelService.getModel(modelCode).getCode(), ViewType.VIEW);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return viewWrapper.e2vList(viewList);
    }

    /**
     * 获取model下的查看视图列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/shadowViews")
    public List<ViewVO> shadowViews(String modelCode, String showType, String viewType, int editViewType) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<View> viewList = null;
        if (StringUtils.isEmpty(modelCode)) {
            viewList = new ArrayList<View>();
        } else if ("EDIT".equals(viewType) || "VIEW".equals(viewType)) {
            if(isProj){
                viewList=viewService.findProjViewsByAssModelCode(modelService
                                .getModel(modelCode).getCode(), editViewType,
                        ViewType.EDIT, ViewType.VIEW);
            }else{
                viewList = viewService.findViewsByAssModelCode(modelService
                                .getModel(modelCode).getCode(), editViewType,
                        ViewType.EDIT, ViewType.VIEW);
            }
        } else if ("LIST".equals(viewType)
                || "REFERENCE".equals(viewType)) {
            if(isProj){
                viewList=viewService.findProjViewsByAssModelCode(modelService
                                .getModel(modelCode).getCode(), ViewType.LIST,
                        ViewType.REFERENCE);
            }else {
                viewList = viewService.findViewsByAssModelCode(modelService
                                .getModel(modelCode).getCode(), ViewType.LIST,
                        ViewType.REFERENCE);
            }
        } else if ("MNECODE".equals(viewType)) {
            if(isProj){
                viewList=viewService.findProjViewsByAssModelCode(modelService
                        .getModel(modelCode).getCode(), ViewType.MNECODE);
            }else {
                viewList = viewService.findViewsByAssModelCode(modelService
                        .getModel(modelCode).getCode(), ViewType.MNECODE);
            }
        } else if ("EXTRA".equals(viewType)) {
            if(isProj){
                viewList=viewService.findProjViewsByAssModelCode(modelService
                        .getModel(modelCode).getCode(), ViewType.EXTRA);
            }else{
                viewList = viewService.findViewsByAssModelCode(modelService
                        .getModel(modelCode).getCode(), ViewType.EXTRA);
            }
        } else {
            viewList = new ArrayList<View>();
        }

        for (Iterator<View> it = viewList.iterator(); it.hasNext(); ) {
            View v = it.next();
            if ((ShowType.valueOf(showType) != v.getShowType())
                    || (null != v.getIsShadow() && v.getIsShadow())) {
                it.remove();
                viewList.remove(v);
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return viewWrapper.e2vList(viewList);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/batchControlPrintViews")
    public List<ViewVO> batchControlPrintViews(String modelCode) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<View> viewList = viewService.findViewsByAssModelCode(
                modelService.getModel(modelCode).getCode(), ViewType.VIEW,
                ViewType.EDIT);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return viewWrapper.e2vList(viewList);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/modelRefViews")
    public List<ViewVO> modelReferenceViews(String modelCode, String viewType) {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<View> refviews = new ArrayList<View>();
        if ("EDIT".equals(viewType)) {
            refviews = viewService.findViewsByAssModelCode(modelService
                    .getModel(modelCode).getCode(), ViewType.REFERENCE);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return viewWrapper.e2vList(refviews);
    }

    /**
     * 保存视图配置信息
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/save-config")
    public ExtraViewVO saveConfig(HttpServletRequest request) throws Exception {
        ExtraView ev = DtoUtils.getExtraView(request);
//        FastQueryJson fqj = DtoUtils.getFastQueryJson(getRequest());
//        AdvQueryJson aqj = DtoUtils.getAdvQueryJson(getRequest());
        Boolean hasCustomSection = false;
        if (!ObjectUtils.isEmpty(request.getParameter("hasCustomSection"))){
            hasCustomSection = Boolean.parseBoolean(request.getParameter("hasCustomSection"));
        }
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
            ev.setProjFlag(true);
        }
        if (ev.getCode() == null || ev.getCode().length() == 0) {
            ev.setCode(ev.getView().getCode());
        }
        Map<String, Object> argsMap = new HashMap<String, Object>();
        argsMap.put("fieldConfig", request.getParameter("fieldConfig"));
        argsMap.put("delCellIds", request.getParameter("delCellIds"));
        argsMap.put("delEventIds", request.getParameter("delEventIds"));
        argsMap.put("delValidateIds", request.getParameter("delValidateIds"));
        argsMap.put("btDelCellIds", request.getParameter("btDelCellIds"));
        argsMap.put("fqj", request.getParameter("fqj"));
        argsMap.put("aqj", request.getParameter("aqj"));// mkp-高级查询
        argsMap.put("fieldSelectionRange", request.getParameter("fieldSelectionRange"));
        if(request.getRequestURI().endsWith("publish-config")){
            argsMap.put("needBackup", true);
        } else {
            argsMap.put("needBackup", false);
        }
        argsMap.put("hasCustomSection", hasCustomSection);
        viewService.saveViewConfig(ev, argsMap);
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return extraViewWrapper.e2v(ev);
    }

    /**
     * 保存增强型视图快速查询配置信息
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/save-fastqueryconfig")
    public FastQueryJsonVO saveFastQueryConfig(HttpServletRequest request, String fieldConfig, String fastDelCells) throws Exception {
        //FastQueryJson fqj = DtoUtils.getFastQueryJson(getRequest());
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        FastQueryJson fqj = new FastQueryJson();
        fqj.setCode(request.getParameter("fqj.code"));
        View view = new View();
        view.setCode(request.getParameter("fqj.view.code"));
        fqj.setView(view);
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
        fqj.setLayoutName(request.getParameter("fqj.layoutName"));
        Model model = new Model();
        model.setCode(request.getParameter("fqj.targetModel.code"));
        fqj.setTargetModel(model);
        viewService.saveExtraFastQueryJson(fqj, false);
        if (fieldConfig != null && !fieldConfig.equals("")) {
            fastQueryJsonService.saveFields(fqj, fieldConfig);
        }
        if (fastDelCells != null && !fastDelCells.equals("")) {
            fieldService.deleteFieldByCellCodes(fqj.getCode(), fastDelCells);
        }
        fqj = fastQueryJsonService.getFastQueryJson(fqj.getCode());
        viewService.saveExtraFastQueryJson(fqj, true);
        fqj = fastQueryJsonService.getFastQueryJson(fqj.getCode());
        fqj.setFields(fqj.getFields());
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return fastQueryJsonWrapper.e2v(fqj);
    }

    /**
     * 保存增强型视图快速查询配置信息
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/save-advqueryconfig")
    public AdvQueryJsonVO saveAdvQueryConfig(HttpServletRequest request, String fieldConfig, String advDelCells) throws Exception {
        AdvQueryJson aqj = new AdvQueryJson();
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        aqj.setCode(request.getParameter("aqj.code"));
        View view = new View();
        view.setCode(request.getParameter("aqj.view.code"));
        aqj.setView(view);
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
        aqj.setLayoutName(request.getParameter("aqj.layoutName"));
        Model model = new Model();
        model.setCode(request.getParameter("aqj.targetModel.code"));
        aqj.setTargetModel(model);
        viewService.saveExtraAdvQueryJson(aqj, false);
        if (fieldConfig != null && !fieldConfig.equals("")) {
            advQueryJsonService.saveFields(aqj, fieldConfig);
        }
        if (advDelCells != null && !advDelCells.equals("")) {
            fieldService.deleteFieldByCellCodes(aqj.getCode(), advDelCells);
        }
        aqj = advQueryJsonService.getAdvQueryJson(aqj.getCode());
        viewService.saveExtraAdvQueryJson(aqj, true);
        aqj = advQueryJsonService.getAdvQueryJson(aqj.getCode());
        aqj.setFields(aqj.getFields());
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return advQueryJsonWrapper.e2v(aqj);
    }

    /**
     * 查询系统编辑
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/system-code-types")
    public Map systemCodeTypes(String moduleCode) throws Exception {
        Map<String, Map<String, Map<String, String>>> systemCodeMap = systemCodeInfoService.getSystemEntityMapByGroup(moduleCode);
        return systemCodeMap;
    }

    /**
     * 先保存视图配置信息再发布视图
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/publish-config")
    public ExtraViewVO publish(HttpServletRequest request) throws Exception {
        ExtraView ev = DtoUtils.getExtraView(request);
        FastQueryJson fqj = DtoUtils.getFastQueryJson(getRequest());
        AdvQueryJson aqj = DtoUtils.getAdvQueryJson(getRequest());
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (ev.getCode() == null || ev.getCode().length() == 0) {
            ev.setCode(ev.getView().getCode());
        }
        saveConfig(request);


        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        View view = viewService.getView(ev.getView().getCode());

        viewService.viewPublish(view, ev);
        View evView=ev.getView();
        evView.setFqj(fqj);
        evView.setAqj(aqj);
        ev.setView(evView);
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        return extraViewWrapper.e2v(ev);
    }

    /**
     * 获取关联模型的属性和对应的关联模型
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping({"/ec/view/fastQueryProperties", "/ec/view/select_subs"})
    public Map<String, Object> select_subs(String modelCode, String type) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        Model targetModel=modelService.getModel(modelCode);
        List<Property> properties = modelService.findProperties(targetModel);
        List<AssociatedInfo> origAssociatedInfos = modelService.findAssociatedInfos(targetModel);
        Map<String, Object> subInfos = getSubEntitiesAndProperties(targetModel, properties, origAssociatedInfos, type);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return subInfos;
    }
    
    /**
     * 获取模型的属性和关联模型
     *
     * @param
     * @return
     */
    private Map<String, Object> getSubEntitiesAndProperties(Model model, List<Property> properties, List<AssociatedInfo> origAssociatedInfos,
                                                            String type) {
        String modelCode = model.getCode();
        Map<String, Object> subs = new HashMap<String, Object>();
        // 获取实体属性
        List<AssociatedInfo> associatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> oneToManyAssociatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> reverseAssociatedInfos = new ArrayList<AssociatedInfo>();

        if (origAssociatedInfos != null) {
            for (AssociatedInfo asso : origAssociatedInfos) {
                // 以当前模型做为源模型的一对多关联
                // if (null != type && type.equals("fast")) {
                if ((modelCode.equals(asso.getOriginalProperty().getModel()
                        .getCode())
                        && null != asso.getType() && asso.getType() == 3)
                        || (modelCode.equals(asso.getTargetProperty()
                        .getModel().getCode())
                        && !asso.getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(asso.getOriginalProperty()
                                .getModel().getCode()) && asso
                        .getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(asso.getOriginalProperty().getModel()
                                .getCode()))) {
                    asso.getOriginalProperty().setModel(model);
                    oneToManyAssociatedInfos.add(asso);
                    continue;
                }
                if (null == asso.getOriginalProperty().getAssociatedProperty()
                        && null != asso.getTargetProperty()
                        && null != asso.getTargetProperty()
                        .getAssociatedProperty()
                        && asso.getTargetProperty().getAssociatedProperty()
                        .getCode()
                        .equals(asso.getOriginalProperty().getCode())) {
                    continue;
                }
                // 关联字段移除
                for (int i = 0; i < properties.size(); i++) {
                    if (properties
                            .get(i)
                            .getDisplayName()
                            .equals(asso.getOriginalProperty().getDisplayName())) {
                        properties.remove(i);
                        break;
                    }
                }

                asso.getOriginalProperty().setModel(model);
                if (model.getInherentCommonFlag() != null
                        && model.getInherentCommonFlag()
                        && ("mainObj".equalsIgnoreCase(asso
                        .getOriginalProperty().getName()) || "linkId"
                        .equalsIgnoreCase(asso.getOriginalProperty()
                                .getName()))) {
                    continue;
                }
                associatedInfos.add(asso);
            }
        }
        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property p = it.next();
            if (p.getType() == DbColumnType.PASSWORD
                    || p.getType() == DbColumnType.OFFICE
                    || p.getType() == DbColumnType.LAYER) {
                it.remove();
                properties.remove(p);
            }
        }
        if (null != type
                && (type.equals("list") || type.equals("fast") || type
                .equals("adv"))) {
            for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                Property p = it.next();
                if (!p.getIsUsedForList()) {
                    it.remove();
                    properties.remove(p);
                } else if (p.getName().equalsIgnoreCase("extraCol")) {
                    it.remove();
                    properties.remove(p);
                } else if (p.getName().equals("status")
                        && null != model.getIsMain() && model.getIsMain()
                        && model.getEntity().getWorkflowEnabled()) {
                    p.setType(DbColumnType.OBJECT);
                    AssociatedInfo associatedInfo = new AssociatedInfo();
                    Property statusProperty = modelService
                            .getProperty(STATUS_CODE);
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
        if (null != type && type.equals("fast")) {
            for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                Property p = it.next();
                if (p.getType() == DbColumnType.LONGTEXT
                        || p.getType() == DbColumnType.OFFICE
                        || p.getType() == DbColumnType.PROPERTYATTACHMENT) {
                    it.remove();
                    properties.remove(p);
                }

            }
        }
        if (model.getIsMain() != null && model.getIsMain()
                && model.getEntity() != null
                && model.getEntity().getWorkflowEnabled() != null
                && model.getEntity().getWorkflowEnabled()) {
            List<AssociatedInfo> inherentAssos = modelService
                    .findInherentAssociatedInfos(AssociatedInfo.ONE_TO_MANY);
            List<Property> idProperties = modelService.findProperties(
                    Restrictions.eq("model", model),
                    Restrictions.eq("name", "id"));
            List<Property> tableInfoIdProperties = modelService.findProperties(
                    Restrictions.eq("model", model),
                    Restrictions.eq("name", "tableInfoId"));
            for (AssociatedInfo item : inherentAssos) {
                if ("linkId".equals(item.getTargetProperty().getName())
                        && null != tableInfoIdProperties
                        && !tableInfoIdProperties.isEmpty()) {
                    item.setOriginalProperty(tableInfoIdProperties.get(0));
                } else if (null != idProperties && idProperties.size() > 0) {
                    item.setOriginalProperty(idProperties.get(0));
                }
            }
            oneToManyAssociatedInfos.addAll(inherentAssos);
        }
        Boolean projectFlag = ProjectFlagHolder.getInstance().getProjFlag().get();
        if (projectFlag && null != type && type.equals("list")) {
        	for (Iterator<Property> it = properties.iterator(); it.hasNext();) {
        		Property p = it.next();
        		if (p.getIsCustom() && DbColumnType.OBJECT.equals(p.getType())) {
        			CustomPropertyModelMapping customPropertyModelMapping = modelService.getAssociatedCustomPropertyModelMapping(p.getCode());
        			if (null != customPropertyModelMapping) {
        				AssociatedInfo associatedInfo = new AssociatedInfo();
        				associatedInfo.setTargetProperty(customPropertyModelMapping.getAssociatedProperty());
                        associatedInfo.setOriginalProperty(p);
                        associatedInfo.setEcEnv(EcEnv.project);
                        associatedInfo.setType(customPropertyModelMapping.getAssociatedType());
                        associatedInfo.setIsMainAssociated(false);
                        associatedInfo.setValid(true);
                        associatedInfo.setVersion(0);
                        associatedInfos.add(associatedInfo);
                        it.remove();
        			}
        		}
        	}
        }

        subs.put("properties", properties);
        subs.put("associatedInfos", associatedInfos);
        subs.put("oneToManyAssociatedInfos", oneToManyAssociatedInfos);
        subs.put("reverseAssociatedInfos", reverseAssociatedInfos);
//		if (view != null) {
//			if (!subs.containsKey("mainEntityName")) {
//				subs.put("mainEntityName", view.getAssModel().getModelName());
//			}
//		}
        return subs;
    }

    /**
     * 重置关连的源和目标
     *
     * @return AssociatedInfo
     * @throws Exception
     */
    private AssociatedInfo resetOriginalAndTarget(Property originalProp,
                                                  Property targetProp) {
        AssociatedInfo asso = new AssociatedInfo();
        asso.setOriginalProperty(originalProp);
        asso.setTargetProperty(targetProp);
        return asso;
    }

    /**
     * 获取历史记录的列表
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/listBackupView")
    public Page<BackupView> listBackupView(Page<BackupView> backupViewPage, String viewCode) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        backupViewPage = viewService
                .getBackupViewList(backupViewPage, viewCode);
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return backupViewPage;
    }

    /**
     * 还原历史记录
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/restoreView")
    public String restoreView() throws Exception {
        ResponseMsg response = new ResponseMsg(true);
        String backupViewCode = getRequest().getParameter("backupView.code");
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            viewService.restoreView(backupViewCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setSuccess(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    /**
     * 转向到历史记录列表
     *
     * @return
     * @throws SQLException
     */
    @RequestMapping(value = "/ec/view/remindBackupViewList")
    public String remindBackupViewList(ModelMap map,String code) throws SQLException {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        map.addAttribute("isProj", isProj);
        map.addAttribute("code", code);
        return "view/config-backupview-list";
    }

    /**
     * 删除发布历史记录
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/delBackupView")
    public String delBackupView(String code) throws SQLException, JsonProcessingException {
        ResponseMsg response = new ResponseMsg(true);
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        ObjectMapper mapper = new ObjectMapper();
        if (null == code || code.length() == 0) {
            response.setSuccess(false);
            response.setExceptionMsg(InternationalResource.get(
                    "ec.entity.viewaction.nodeletehistorty"));
        } else {
            viewService.deleteBackupView(code);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    @RequestMapping(value = "/ec/view/configRelationModelPropsFrame")
    public String configRelationModelPropsFrame(ModelMap map, String modelCode) {
        Set<Model> relationModels = new HashSet<Model>();
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            // 如果url中isProj为true,则本次请求调用的所有service所使用的sessionFectory切换为proj的表
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (modelCode != null && modelCode.length() > 0) {
            relationModels = modelService.findRelationModels(modelCode,
                    Property.MANY_TO_ONE, Property.ONE_TO_ONE);
            map.addAttribute("relationModels", relationModels);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/config-relationModelProps";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/getSecondLevelAssoModels")
    public Map getSecondLevelAssoModels(String modelCode) {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        if (modelCode != null && modelCode.length() > 0) {
            Set<Model> relationModels = modelService.findRelationModels(modelCode,
                    Property.MANY_TO_ONE, Property.ONE_TO_ONE);
            responseMap.put("dealSuccessFlag", true);
            String modelJson = "{\"models\":[";
            for (Model m : relationModels) {
                modelJson += "{\"code\":\"" + m.getCode() + "\",\"name\":\""
                        + InternationalResource.get(m.getName()) + "\"},";
            }
            if (relationModels.size() > 0) {
                modelJson = modelJson.substring(0, modelJson.length() - 1);
            }
            modelJson += "]}";
            responseMap.put("models", modelJson);
        } else {
            responseMap.put("dealSuccessFlag", false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return responseMap;
    }

    /**
     * 更新EC_EXTRA_VIEW中的CONFIG字段 propertyCode
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/viewconfig-init")
    public String modifyViewConfig(String moduleCode) throws Exception {
        // List<View> viewList = new ArrayList<View>();//无用的局部变量可能与实例属性同名
        // 处理fastQueryJson
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        List<View> viewList = viewService.findAllViews(Restrictions.eq("valid",
                Boolean.TRUE), Restrictions.or(
                Restrictions.eq("type", ViewType.LIST),
                Restrictions.eq("type", ViewType.REFERENCE)));
        for (View view : viewList) {
            try {
                if (view.getFastQueryJson() != null
                        && view.getFastQueryJson().iterator().next()
                        .getQueryConfig() != null
                        && view.getFastQueryJson().iterator().next()
                        .getQueryConfig().startsWith("{")) {
                    view.getFastQueryJson()
                            .iterator()
                            .next()
                            .setQueryConfig(
                                    SerializeUitls
                                            .serializeAsXml(SerializeUitls
                                                    .deserialize(view
                                                            .getFastQueryJson()
                                                            .iterator().next()
                                                            .getQueryConfig())));
                    viewService.saveFastQueryJson(view.getFastQueryJson()
                            .iterator().next());
                }
                if (view.getAdvQueryJson() != null
                        && view.getAdvQueryJson().iterator().next()
                        .getQueryConfig() != null
                        && view.getAdvQueryJson().iterator().next()
                        .getQueryConfig().startsWith("{")) {
                    view.getAdvQueryJson()
                            .iterator()
                            .next()
                            .setQueryConfig(
                                    SerializeUitls
                                            .serializeAsXml(SerializeUitls
                                                    .deserialize(view
                                                            .getAdvQueryJson()
                                                            .iterator().next()
                                                            .getQueryConfig())));
                    viewService.saveAdvQueryJson(view.getAdvQueryJson()
                            .iterator().next());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        try {
            Page<View> page = new Page<View>();
            page.setPageSize(Integer.MAX_VALUE);
            viewService.modifyExtraViewPropertyCode(page, moduleCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setSuccess(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    /**
     * 更新EC_EXTRA_VIEW中的CONFIG字段 处理显示类型、显示格式等
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/extraViewconfig-init")
    public String modifyExtraViewConfig(String moduleCode) throws Exception {
        ResponseMsg response = new ResponseMsg(true);
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            viewService.modifyExtraViewField(moduleCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setSuccess(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    /**
     * 处理Field数据 设置fullPropertyCode值
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/public/ec/view/dealField")
    public String dealFieldData() throws JsonProcessingException {
        ResponseMsg response = new ResponseMsg(true);
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            fieldService.dealFieldData();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setSuccess(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    /**
     * 为了获取dispatcher，模拟一个请求
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/public/ec/view/deployGetDispatcher")
    public void deployGetDispatcher() {
//        String moduleCodes[] = getRequest().getParameterValues("moduleCodes");
//        PrintWriter out = null;
//        try {
//            Module[] modules = new Module[moduleCodes.length];
//            Module module = null;
//            for (int i = 0; i < moduleCodes.length; i++) {
//                module = moduleService.getModule(moduleCodes[i]);
//                generateService.generateProjViews(module, out, false);
//                modules[i] = module;
//            }
//            generateService.publishProjViews(null, out, modules);
//        } catch (Exception e) {
//            log.error("工程期视图自动发布失败:" + e.getMessage());
//        }
    }

    /**
     * 处理Field数据 设置DbColumnType值
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/public/ec/view/dealFieldColumnType")
    public String dealFieldColumnType() throws JsonProcessingException {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        try {
            fieldService.dealFieldColumnType();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setSuccess(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return mapper.writeValueAsString(response);
    }

    /**
     * 进入编码格式配置页面
     *
     * @return
     */
    @RequestMapping({"/ec/property/select_property", "/ec/property/select_dateproperty", "/public/property/select_property"})
    public String select_property(ModelMap map, @RequestParam("model.code")String modelCode) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        map.addAttribute("isProj",isProj);
        if (modelCode != null) {
            Model model = modelService.getModelWithProperties(modelCode);
            Set<Property> properties = model.getProperties();
            Iterator<Property> iterator = properties.iterator();
            while (iterator.hasNext()) {
                Property p = iterator.next();
                if (p.getIsCustom()) {
                    iterator.remove();
                }
            }
            model.setAssociatedInfos(modelService
                    .findAssociatedInfoNotIncludeBackAsso(model));
            map.addAttribute("model", model);

        }
        if ("/ec/property/select_dateproperty".equals(getRequest().getRequestURI())) {
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
            return "property/select_dateproperty";
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "property/select_property";
    }

    @RequestMapping(value = "/ec/view/config-select-property")
    public String configSelectProperty(ModelMap map, @RequestParam("model.code")String modelCode) throws Exception {
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        map.addAttribute("isProj",isProj);
        if (modelCode != null) {
            Model model = modelService.getModelWithProperties(modelCode);
            model.setAssociatedInfos(modelService
                    .findAssociatedInfoNotIncludeBackAsso(model));
            map.addAttribute("modelCode", modelCode);
            map.addAttribute("model", model);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/config-select-property";
    }

    @RequestMapping(value = "/ec/view/config-select-model")
    public String configSelectModel(ModelMap map, @RequestParam("model.code")String modelCode) {
        Boolean hasAssModel = false;
        Boolean isProj = Boolean.valueOf(getRequest().getParameter("isProj"));
        if (isProj) {
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        map.addAttribute("isProj",isProj);
        if (modelCode != null) {
            Model model = modelService.getModelWithProperties(modelCode);
            if (model.getProperties() != null
                    && model.getProperties().size() > 0) {
                for (Property p : model.getProperties()) {
                    if ((p.getIsInherent() == null || !p.getIsInherent())
                            && p.getAssociatedProperty() != null
                            && (p.getAssociatedType() == Property.ONE_TO_ONE || p
                            .getAssociatedType() == Property.MANY_TO_ONE)
                            && !"sysbase_1.0".equals(p.getAssociatedProperty()
                            .getModel().getEntity().getModule()
                            .getCode())) { // 固有基础模型除外
                        hasAssModel = true;
                        break;
                    }
                }
            }
            map.addAttribute("model", model);
            map.addAttribute("hasAssModel", hasAssModel);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return "view/config-select-model";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/view/associatedModels")
    public List<PropertyVO> associatedModels(@RequestParam("model.code")String modelCode) {
        List<Property> assProperties = new ArrayList<Property>();
        if (modelCode != null) {
            Model model = modelService.getModelWithProperties(modelCode);
            if (model.getProperties() != null
                    && model.getProperties().size() > 0) {
                for (Property p : model.getProperties()) {
                    if ((p.getIsInherent() == null || !p.getIsInherent())
                            && p.getAssociatedProperty() != null
                            && (p.getAssociatedType() == Property.ONE_TO_ONE || p
                            .getAssociatedType() == Property.MANY_TO_ONE)
                            && !"sysbase_1.0".equals(p.getAssociatedProperty()
                            .getModel().getEntity().getModule()
                            .getCode())) { // 固有基础模型除外
                        assProperties.add(modelService.getPropertyWithModel(p.getCode()));
                    }
                }
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return propertyWrapper.e2vList(assProperties);
    }

    /**
     * 还原历史记录
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/getOperateInfo")
    public Map getMenuOperateInfo(View view) throws Exception {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        
        if (null != view.getPermissionCode()
                && view.getPermissionCode().length() > 0) {
            String opCode = view.getEntity().getCode() + "_"
                    + view.getPermissionCode();
            MenuOperate mo = viewService.getMenuOperateByCode(opCode,
                    getCurrentCompanyId());
            if (null != mo) {
                responseMap.put("dealSuccess", true);
                responseMap.put("url", mo.getUrl());
                responseMap.put("displayName", mo.getName());
                responseMap.put("name", InternationalResource.get(mo.getName()));
            } else {
                responseMap.put("dealSuccess", false);
            }
        }
        return responseMap;
    }

    /**
     * 获取关联模型的属性和对应的关联模型
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/export/view/select_subs")
    public Map export_select_subs(String modelCode, String viewCode) throws Exception {
        Map<String, Object> subInfos = getExportSubEntitiesAndProperties(modelCode, "fast",
                viewCode);
        return subInfos;
    }

    /**
     * 获取模型的属性和关联模型
     *
     * @param
     * @return
     */
    private Map<String, Object> getExportSubEntitiesAndProperties(
            String modelCode, String type, String viewCode) {
        Map<String, Object> subs = new HashMap<String, Object>();

//        Model model = modelServiceFoundation.getModel(modelCode);
//        // 获取实体属性
//        List<Property> properties = modelServiceFoundation
//                .findProperties(model);

        Model model = modelService.getModel(modelCode);
        // 获取实体属性
        List<Property> properties = modelService.findProperties(model);
        List<String> runningCustomPropertyCode = modelService
                .getRunningCustomProperties(model.getCode());
        // 获取已启用自定义字段
        List<String> runningCustomProperty = modelService
                .getRunningCustomProperties(modelCode);
        List<AssociatedInfo> associatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> oneToManyAssociatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> reverseAssociatedInfos = new ArrayList<AssociatedInfo>();
        List<AssociatedInfo> origAssociatedInfos = modelService
                .findAssociatedInfos2(model);
        if (origAssociatedInfos != null) {
            for (AssociatedInfo asso : origAssociatedInfos) {
                // 以当前模型做为源模型的一对多关联
                // if (null != type && type.equals("fast")) {
                if ((modelCode.equals(asso.getOriginalProperty().getModel()
                        .getCode())
                        && null != asso.getType() && asso.getType() == 3)
                        || (modelCode.equals(asso.getTargetProperty()
                        .getModel().getCode())
                        && !asso.getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(asso.getOriginalProperty()
                                .getModel().getCode()) && asso
                        .getTargetProperty()
                        .getModel()
                        .getCode()
                        .equals(asso.getOriginalProperty().getModel()
                                .getCode()))) {
                    asso.getOriginalProperty().setModel(model);
                    oneToManyAssociatedInfos.add(asso);
                    continue;
                }
                if (null == asso.getOriginalProperty().getAssociatedProperty()
                        && null != asso.getTargetProperty()
                        && null != asso.getTargetProperty()
                        .getAssociatedProperty()
                        && asso.getTargetProperty().getAssociatedProperty()
                        .getCode()
                        .equals(asso.getOriginalProperty().getCode())) {
                    continue;
                }

                for (int i = 0; i < properties.size(); i++) {
                    if (properties.get(i).getIsCustom()
                            && !runningCustomPropertyCode.contains(properties
                            .get(i).getCode())) {// 未启用自定义字段移除
                        properties.remove(i);
                    }
                }

                for (int i = 0; i < properties.size(); i++) {
                    if (properties
                            .get(i)
                            .getDisplayName()
                            .equals(asso.getOriginalProperty().getDisplayName())) {// 关联字段移除
                        properties.remove(i);
                        break;
                    }
                }

                asso.getOriginalProperty().setModel(model);
                if (model.getInherentCommonFlag() != null
                        && model.getInherentCommonFlag()
                        && ("mainObj".equalsIgnoreCase(asso
                        .getOriginalProperty().getName()) || "linkId"
                        .equalsIgnoreCase(asso.getOriginalProperty()
                                .getName()))) {
                    continue;
                }
                associatedInfos.add(asso);
            }
        }
        for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
            Property p = it.next();
            if (p.getType() == DbColumnType.PASSWORD
                    || p.getType() == DbColumnType.OFFICE) {
                it.remove();
                properties.remove(p);
            }

        }
        if (null != type && type.equals("fast")) {
            for (Iterator<Property> it = properties.iterator(); it.hasNext(); ) {
                Property p = it.next();
                if (p.getType() == DbColumnType.LONGTEXT
                        || p.getType() == DbColumnType.OFFICE
                        || p.getType() == DbColumnType.PROPERTYATTACHMENT) {
                    it.remove();
                    properties.remove(p);
                }

            }
        }

        if (model.getIsMain() != null && model.getIsMain()
                && model.getEntity() != null
                && model.getEntity().getWorkflowEnabled() != null
                && model.getEntity().getWorkflowEnabled()) {
            List<AssociatedInfo> inherentAssos = modelService
                    .findInherentAssociatedInfos(AssociatedInfo.ONE_TO_MANY);
            List<Property> idProperties = modelService
                    .findProperties(Restrictions.eq("model", model),
                            Restrictions.eq("name", "id"));
            List<Property> tableInfoIdProperties = modelService
                    .findProperties(Restrictions.eq("model", model),
                            Restrictions.eq("name", "tableInfoId"));
            for (AssociatedInfo item : inherentAssos) {
                if ("linkId".equals(item.getTargetProperty().getName())) {
                    item.setOriginalProperty(tableInfoIdProperties.get(0));
                } else {
                    item.setOriginalProperty(idProperties.get(0));
                }
            }
            oneToManyAssociatedInfos.addAll(inherentAssos);
        }

        if (null != viewCode && !"".equals(viewCode)) {
            List<Property> tempPropertyies = new ArrayList<Property>();
            List<Field> fields = fieldService.getFields(viewCode);
            for (Iterator<Field> it = fields.iterator(); it.hasNext(); ) {
                Field field = it.next();
                if (field.getProperty() != null) {
                    for (Iterator<Property> iter = properties.iterator(); iter
                            .hasNext(); ) {
                        Property p = iter.next();
                        if (field.getProperty().equals(p)
                                && !p.getName().equals("id")
                                && !field.getDisplayName().equals(
                                p.getDisplayName())) {
                            Property tempProperty = p;
                            iter.remove();
                            tempProperty.setDisplayName(field.getDisplayName());
                            tempPropertyies.add(tempProperty);
                            break;
                        }
                    }
                }
                break;
            }
            if (tempPropertyies.size() > 0) {
                properties.addAll(tempPropertyies);
            }
        }

        subs.put("properties", properties);
        subs.put("associatedInfos", associatedInfos);
        subs.put("oneToManyAssociatedInfos", oneToManyAssociatedInfos);
        subs.put("reverseAssociatedInfos", reverseAssociatedInfos);
//		if (view != null) {
//			if (!subs.containsKey("mainEntityName")) {
//				subs.put("mainEntityName", view.getAssModel().getModelName());
//			}
//		}
        return subs;
    }

    /**
     * 检查数据分类能否默认
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/wouldDefaultChecked")
    public Map wouldDefaultChecked(DataClassific dataClassific, View view) throws Exception {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        if (null != dataClassific && null != view && null != view.getCode()) {
            List<DataClassific> dataClassifics = viewService
                    .getDataClassificInView(dataClassific, view);
            if (dataClassifics.size() > 0) {
                DataClassific dataClassific1 = dataClassifics.get(0);
                dataClassific1.setDisplayName(InternationalResource
                        .get(dataClassific1.getDisplayName()));
                responseMap.put("isExist", true);
                responseMap.put("dataClassific", dataClassific1);
            } else {
                responseMap.put("isExist", false);
            }
        }
        return responseMap;
    }

    /**
     * 返回视图json
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/viewJson")
    public String viewJson(View view) throws Exception {
        view = viewService.getView(view.getCode());
        ExtraView ev = null;
        if (view.getIsShadow()) {
            View shadowView = view.getShadowView();
            if (null != shadowView) {
                ev = shadowView.getExtraView();
            }
        } else {
            ev = view.getExtraView();
        }
        String config = null;
        if (null != ev) {
            if (view.getType() != ViewType.MNECODE
                    && view.getShowType() != ShowType.LAYOUT) {
                /* 已经配置过 */
                config = ev.getViewJson();
//				config = viewServiceFoundation.viewJsonInternational(config);

            }
        }
        return config;
    }

    /**
     * 获取entity下的参照视列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/referenceMobileViews")
    public List<ViewVO> referenceMobileViews(String modelCode) {
        List<View> viewList = null;
        if (modelCode != null && modelCode.length() > 0) {
            if ("sysbase_1.0_staff_base_staff".equals(modelCode)
                    || "sysbase_1.0_position_base_position".equals(modelCode)
                    || "sysbase_1.0_department_base_department"
                    .equals(modelCode)) {
                viewList = viewService.findViewsByAssModelCode(modelService
                        .getModel(modelCode).getCode(), ViewType.REFERENCE);
            } else {
                viewList = viewService.findMobileViewsByAssModelCode(
                        modelService.getModel(modelCode).getCode(),
                        ViewType.REFERENCE);
            }
        }
        if (null != viewList && !viewList.isEmpty()) {
            for (Iterator<View> it = viewList.iterator(); it.hasNext(); ) {
                View v = it.next();
                if (v.getShowType() == ShowType.LAYOUT
                        || v.getShowType() == ShowType.LAYOUT2) {
                    // viewList.remove(v);
                    it.remove();
                }
            }
        }
        return viewWrapper.e2vList(viewList);
    }

    /**
     * 获取model下的查看视图列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/view/viewMobileView")
    public List<ViewVO> viewMobileView(String modelCode) {
        List<View> viewList = viewService.findMobileViewsByAssModelCode(modelService
                .getModel(modelCode).getCode(), ViewType.VIEW);
        return viewWrapper.e2vList(viewList);
    }

    /**
     * 获取model下的查看视图列表
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/foundation/customProp/getPropDisplayName")
    public Map<String, Object>  getPropDisplayName(HttpServletRequest request) {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        String modelCode=request.getParameter("modelCode");
        String propLayRec=request.getParameter("propLayRec");
        if(null != modelCode && !modelCode.equals("") && null != propLayRec && !propLayRec.equals("")){
            responseMap.put("dealSuccessFlag", true);
            responseMap.put("propDisplayName", viewService.findPropDisplayName(propLayRec, modelCode,"ec"));
        }
        return responseMap;
    }

}
