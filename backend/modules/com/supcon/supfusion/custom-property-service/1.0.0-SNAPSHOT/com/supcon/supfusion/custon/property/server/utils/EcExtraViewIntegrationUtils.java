package com.supcon.supfusion.custon.property.server.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.custon.property.common.enums.ShowFormat;
import com.supcon.supfusion.custon.property.common.enums.ViewType;
import com.supcon.supfusion.custon.property.common.utils.DateUtils;
import com.supcon.supfusion.custon.property.dao.entity.*;
import com.supcon.supfusion.custon.property.dao.mappers.*;
import com.supcon.supfusion.custon.property.dao.utils.SerializeUitls;
import com.supcon.supfusion.custon.property.dao.utils.XmlUtils;
import com.supcon.supfusion.custon.property.server.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * 视图布局配置信息与字段信息整合公共类
 *
 * @author zhuyuyin
 * @version $Id$
 */
@Component
public class EcExtraViewIntegrationUtils {

    private static final String[] PAGE_EVENTS = {"onload", "onsave", "beforeSave", "afterSave", "beforeSubmit", "afterSubmit"};
    private static final Logger LOGGER = LoggerFactory.getLogger(EcExtraViewIntegrationUtils.class);

    private Map<String, Object> fieldEventMap = new HashMap<String, Object>();
    private Map<String, Object> buttonEventMap = new HashMap<String, Object>();
    private Map<String, Object> validateMap = new HashMap<String, Object>();
    private Map<String, Object> fastQueryJsonMap = new HashMap<String, Object>();
    private Map<String, Object> advQueryJsonMap = new HashMap<String, Object>();

    private long timestamp = 0;
    private long random_num = 0;

    @Autowired
    private ExtraViewService extraViewService;
    @Autowired
    private FastQueryJsonService fastQueryJsonService;
    @Autowired
    private ViewServiceFoundation viewServiceFoundation;
    @Autowired
    private AdvQueryJsonMapper advQueryJsonMapper;
    @Autowired
    private EventService eventService;
    @Autowired
    private ValidateService validateService;
    @Autowired
    private DataGridService dataGridService;

    /**
     * 视图布局配置信息与字段信息整合
     *
     * @param obj
     * @param infoMap
     * @return
     */
    public String ecExtraViewIntegrationBuild(Object obj, Map<String, List<?>> infoMap) {
        String config = "";
        if (obj instanceof View) {
            View view = (View) obj;
            ViewType viewType = view.getType();
            boolean isExtra = false;    //如果是增强型视图    isExtra为true,否则为false
            if (view.getEditViewType() == 1) {
                if (viewType == ViewType.VIEW || viewType == ViewType.EDIT || viewType == ViewType.EXTRA || ((viewType == ViewType.REFERENCE || viewType == ViewType.LIST) && view.getMobile())) {
                    isExtra = true;
                }
            } else {
                isExtra = false;
            }
            String evCode = view.getExtraViewCode();
            ExtraView ev = null;
            if (StringUtils.isNotBlank(evCode)) {
                ev = extraViewService.getById(evCode);
            }
            if (null == ev) {
                ev = new ExtraView();
            }
            if (null != view.getIsShadow() && view.getIsShadow() && StringUtils.isNotBlank(view.getShadowViewCode())) {
                View shadowView = viewServiceFoundation.getById(view.getShadowViewCode());
                if (StringUtils.isNotBlank(shadowView.getExtraViewCode())) {
                    ExtraView extraView = extraViewService.getById(shadowView.getExtraViewCode());
                    ev.setConfig(extraView.getConfig());
                }
            }
            if (null != ev && null != ev.getConfig() && ev.getConfig().length() > 0) {
                config = ev.getConfig();
                if (isExtra) {
                    if (view.getFastQueryJsonCode() != null && !view.getFastQueryJsonCode().isEmpty()) {
                        List<FastQueryJson> fastQueryJsons = fastQueryJsonService.getFastQueryJsonByViewCode(view.getCode());
                        infoMap.put("fastQueryJsons", fastQueryJsons);
                    }
                    List<AdvQueryJson> advQueryJsons = advQueryJsonMapper.selectList(new LambdaQueryWrapper<AdvQueryJson>()
                            .eq(AdvQueryJson::getViewCode, view.getCode()));
                    if (advQueryJsons != null) {
                        infoMap.put("advQueryJsons", advQueryJsons);
                    }
                }
                config = ecConfigBuild(config, infoMap, isExtra);
            }
        } else if (obj instanceof DataGrid) {
            DataGrid dg = (DataGrid) obj;
            if (null != dg.getConfig() && dg.getConfig().length() > 0) {
                config = dg.getConfig();
                config = ecConfigBuild(config, infoMap, false);
            }
        } else if (obj instanceof FastQueryJson) {
            FastQueryJson fqj = (FastQueryJson) obj;
            if (null != fqj.getQueryConfig() && fqj.getQueryConfig().length() > 0) {
                config = fqj.getQueryConfig();
                config = ecExtraQueryConfigBuild(config, infoMap, "fastQuery");
            }
        } else if (obj instanceof AdvQueryJson) {
            AdvQueryJson aqj = (AdvQueryJson) obj;
            if (null != aqj.getQueryConfig() && aqj.getQueryConfig().length() > 0) {
                config = aqj.getQueryConfig();
                config = ecExtraQueryConfigBuild(config, infoMap, "advQuery");
            }
        }

        return config;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String ecConfigBuild(String config, Map<String, List<?>> infoMap, boolean isExtra) {
        Map configMap = null;
        if (infoMap != null && !infoMap.isEmpty()) {
            Map<String, Object> fullMap = getFullFieldInfoMap(infoMap);


            configMap = (Map) SerializeUitls.deserialize(config);

            if (configMap != null && !configMap.isEmpty()) {

                return getFullConfig(configMap, fullMap, isExtra);

            }
        } else {
            configMap = (Map) SerializeUitls.deserialize(config);
        }
        return SerializeUitls.serializeAsXml(configMap);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String ecExtraQueryConfigBuild(String config, Map<String, List<?>> infoMap, String queryMethod) {
        Map configMap = null;
        if (infoMap != null && !infoMap.isEmpty()) {
            Map<String, Object> fullMap = getFullFieldInfoMap(infoMap);
            configMap = (Map) SerializeUitls.deserialize(config);

            if (configMap != null && !configMap.isEmpty()) {
                if (null != queryMethod && queryMethod.equals("fastQuery")) {
                    return getExtraFastQueryConfig(configMap, fullMap);
                } else if (null != queryMethod && queryMethod.equals("advQuery")) {
                    return getExtraAdvQueryConfig(configMap, fullMap);
                }
            }
        } else {
            configMap = (Map) SerializeUitls.deserialize(config);
        }
        return SerializeUitls.serializeAsXml(configMap);
    }

    /**
     * 组织事件信息
     *
     * @param cell
     * @param eventSet
     */
    private void ecCellEventBuilder(Map<String, Object> cell, Set<Event> eventSet) {
        String funcname = "";
        String funcbody = "";
        String funcbody_es5 = "";
        String callbackname = "";
        String callbackbody = "";
        String callbackbody_es5 = "";
        if (eventSet != null && !eventSet.isEmpty()) {
            for (Event event : eventSet) {
                if (event.getName() != null && event.getName().length() > 0) {
                    if (EventUtils.isCallBack(event.getName())) {
                        callbackname = EventUtils.getCallBackName(event.getName());// callback事件
                        callbackbody = event.getFunction();
                        callbackbody_es5 = event.getFunction_es5();
                    } else {
                        funcname += " " + event.getName(); // 其他事件
                        funcbody += "@@@@" + event.getFunction();
                        funcbody_es5 += "@@@@" + event.getFunction_es5();
                    }
                }
            }
            if (funcname != null && funcname.length() > 0) {
                funcname = funcname.substring(1);
            }
            if (funcbody != null && funcbody.length() > 0) {
                funcbody = funcbody.substring(4);
            }
            if (funcbody_es5 != null && funcbody_es5.length() > 0) {
                funcbody_es5 = funcbody_es5.substring(4);
            }
        }
        cell.put("callbackname", callbackname);
        cell.put("callbackbody", callbackbody);
        cell.put("callbackbody_es5", callbackbody_es5);
        cell.put("funcname", funcname);
        cell.put("funcbody", funcbody);
        cell.put("funcbody_es5", funcbody_es5);
    }

    /**
     * 组织验证信息
     *
     * @param cell
     * @param validateSet
     */
    @SuppressWarnings("unchecked")
    private void ecCellValidateBuilder(Map<String, Object> cell, Set<Validate> validateSet) {
        if (validateSet != null && !validateSet.isEmpty()) {
            List<Map<String, Object>> validates = new ArrayList<Map<String, Object>>(); // 初始化validate列表
            for (Validate validate : validateSet) {
                Map<String, Object> validateMap = new HashMap<String, Object>(); // 初始化validate
                // 属性Map
                if (validate.getType() != null && validate.getType().length() > 0) {
                    validateMap.put("type", validate.getType());// 类型
                    validateMap.put("ecEnv", validate.getEcEnv());
                    if (validate.getParams() != null && validate.getParams().length() > 0) {
                        Map<String, Object> paramMap = (Map<String, Object>) SerializeUitls.deserialize(validate.getParams()); // 验证内容
                        validateMap.put("param", paramMap);
                    }
                }
                validates.add(validateMap);
            }
            cell.put("validate", validates);
        }
    }

    /**
     * 根据字段、事件、按钮获取相应属性Map
     *
     * @param infoMap
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<?>> getFieldListMap(Map<String, List<?>> infoMap) {
        if (infoMap != null && !infoMap.isEmpty()) {

            Map<String, List<?>> map = new HashMap<String, List<?>>();
            List<Map<String, Object>> fields = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> buttons = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
            List<?> buttonList = infoMap.get("buttons");
            if (buttonList != null && !buttonList.isEmpty()) {
                for (Object object : buttonList) {
                    Button button = (Button) object;
                    Map<String, Object> buttonMap = getButtonAttributeMap(button);
                    if (buttonMap != null && !buttonMap.isEmpty()) {
                        buttons.add(buttonMap);
                    }
                }
            }

            List<?> fieldList = infoMap.get("fields");
            if (fieldList != null && !fieldList.isEmpty()) {
                for (Object object : fieldList) {
                    Field field = (Field) object;
                    Map<String, Object> fieldProperties = null;
                    try {
                        fieldProperties = ReflectUtils.getDeclaredFieldValues(field);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error(e.getMessage(), e);
                    } catch (IllegalAccessException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(field.getConfig());
                    if (null != fieldMap && !fieldMap.isEmpty()) {
                        Map<String, Object> fieldAttrMap = (Map<String, Object>) fieldMap.get("field");
                        if (null != fieldAttrMap && !fieldAttrMap.isEmpty()) {
                            if (fieldProperties != null) {
                                fieldProperties.putAll(fieldAttrMap);
                            } else {
                                fieldProperties = fieldAttrMap;
                            }
                            fields.add(fieldProperties);
                        }
                    }
                }
            }

            List<?> eventList = infoMap.get("events");
            if (eventList != null && !eventList.isEmpty()) {
                for (Object object : eventList) {
                    Event event = (Event) object;
                    Map<String, Object> eventMap = new HashMap<String, Object>();
                    eventMap.put("name", event.getName());
                    eventMap.put("function", (event.getFunction() == null) ? "" : event.getFunction());
                    eventMap.put("function_es5", (event.getFunction_es5() == null) ? "" : event.getFunction_es5());
                    eventMap.put("layoutCode", event.getLayoutCode());
                    events.add(eventMap);
                }
            }

            if (events != null && !events.isEmpty()) {
                map.put("events", events);
            }
            if (buttons != null && !buttons.isEmpty()) {
                map.put("buttons", buttons);
            }
            if (fields != null && !fields.isEmpty()) {
                map.put("fields", fields);
            }
            return map;
        }

        return null;
    }

    /**
     * 根据Button转换成包含Buttton属性的Map
     *
     * @param button
     * @return
     */
    public Map<String, Object> getButtonAttributeMap(Button button) {
        Map<String, Object> buttonMap = new HashMap<String, Object>();
        if (null != button.getName() && button.getName().length() > 0) {
            buttonMap.put("id", button.getName());
        }
        if (null != button.getDisplayName() && button.getDisplayName().length() > 0) {
            buttonMap.put("namekey", button.getDisplayName());
        }
        if (null != button.getButtonStyle() && button.getButtonStyle().length() > 0) {
            buttonMap.put("buttonstyle", button.getButtonStyle());
        }
        if (null != button.getButtonOperationCode() && button.getButtonOperationCode().length() > 0) {
            buttonMap.put("buttonstyle", button.getButtonOperationCode());
        }
        if (null != button.getIsUseMore()) {
            buttonMap.put("useInMore", button.getIsUseMore());
        }
        if (null != button.getOperateType()) {
            buttonMap.put("operatetype", button.getOperateType());
        }
        if (StringUtils.isNotBlank(button.getViewSelectCode())) {
            buttonMap.put("viewselect", button.getViewSelectCode());
        }
        if (null != button.getIsCallback()) {
            buttonMap.put("iscallback", button.getIsCallback());
        }
        if (null != button.getIsCustomFunc()) {
            buttonMap.put("iscustomfunc", button.getIsCustomFunc());
        }
        if (null != button.getIsPermission()) {
            buttonMap.put("ispermission", button.getIsPermission());
        }
        if (null != button.getIsConfirm()) {
            buttonMap.put("isconfirm", button.getIsConfirm());
        }
        if (null != button.getIsHide()) {
            buttonMap.put("ishide", button.getIsHide());
        }
        if (null != button.getOperateUrl() && button.getOperateUrl().length() > 0) {
            buttonMap.put("operateurl", button.getOperateUrl());
        }
        if (null != button.getConfirmContent() && button.getConfirmContent().length() > 0) {
            buttonMap.put("confirmcontent", button.getConfirmContent());
        }
        if (null != button.getScriptCode() && button.getScriptCode().length() > 0) {
            buttonMap.put("scriptCode", button.getScriptCode());
        }
        if (null != button.getEcEnv()) {
            buttonMap.put("ecEnv", button.getEcEnv());
        }
        return buttonMap;
    }

    /**
     * 将完成的新布局的快速查询、高级查询的queryconfig配置信息拆分为布局信息与字段属性信息
     *
     * @param config
     * @return HashMap {"config":页面布局信息,"fieldConfig":字段属性信息}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> ecSplitByQueryConfig(String config) {
        if (null != config && config.length() > 0) {
            Map<String, Object> splitMap = new HashMap<String, Object>();
            Map configMap = (Map) SerializeUitls.deserialize(config);
            if (null != configMap && !configMap.isEmpty()) {
                Map layout = null;
                if (configMap.get("fastQueryJson") != null) {
                    layout = (Map) configMap.get("fastQueryJson");
                } else if (configMap.get("advQueryJson") != null) {
                    layout = (Map) configMap.get("advQueryJson");
                }
                if (layout != null && !layout.isEmpty()) {
                    List<Map> sections = (List<Map>) layout.get("sections");
                    if (sections != null && !sections.isEmpty()) {
                        String fieldConfig = getFieldConfigFromSections(sections, null);
                        splitMap.put("fieldConfig", fieldConfig);
                        splitMap.put("config", SerializeUitls.serializeAsXml(configMap));
                    }
                }
            }
            return splitMap;
        }
        return null;
    }

    /**
     * 将完成的config配置信息拆分为布局信息与字段属性信息
     *
     * @param config
     * @return HashMap {"config":页面布局信息,"fieldConfig":字段属性信息}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> ecSplitConfig(String config) {
        if (null != config && config.length() > 0) {
            Map<String, Object> splitMap = new HashMap<String, Object>();
            Map configMap = (Map) SerializeUitls.deserialize(config);
            if (null != configMap && !configMap.isEmpty()) {
                Map layout = (Map) configMap.get("layout");
                if (layout != null) {
                    String layoutCode = (layout.get("layoutCode") == null) ? "" : layout.get("layoutCode").toString();
                    if (null != layout && !layout.isEmpty()) {
                        List<Map> events = new ArrayList<Map>();
                        Map pageConfig = (Map) layout.get("pageConfig"); // 页面信息
                        if (pageConfig != null && !pageConfig.isEmpty()) {
                            for (String name : PAGE_EVENTS) {
                                if (pageConfig.containsKey(name)) {
                                    String function = (pageConfig.get(name) == null) ? "" : pageConfig.get(name).toString();
                                    Map event = new HashMap();
                                    event.put("name", name);
                                    event.put("function", function);
                                    event.put("layoutCode", layoutCode);
                                    events.add(event);
                                    pageConfig.remove(name);// 布局中除去onsave事件
                                }
                            }
                        }
                        List<Map> sections = new ArrayList<Map>();
                        List<Map> tabs = (List<Map>) layout.get("tabs");
                        if (null != tabs && !tabs.isEmpty()) {
                            for (Map tab : tabs) {
                                List<Map> tabSections = (List<Map>) tab.get("sections"); // 页签中的section
                                if (tabSections != null && !tabSections.isEmpty()) {
                                    sections.addAll(tabSections);
                                }
                            }
                        }
                        List<Map> sectionSections = (List<Map>) layout.get("sections"); // layout下的section
                        if (sectionSections != null && !sectionSections.isEmpty()) {
                            sections.addAll(sectionSections);
                        }
                        if (sections != null && !sections.isEmpty()) {
                            String fieldConfig = getFieldConfigFromSections(sections, events);
                            splitMap.put("fieldConfig", fieldConfig);
                            splitMap.put("config", SerializeUitls.serializeAsXml(configMap));
                        }
                    }
                }
            }
            return splitMap;
        }
        return null;
    }

    /**
     * 从section中拆分出fieldConfig
     *
     * @param sections
     * @param events   页面事件
     * @return String fieldConfig
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String getFieldConfigFromSections(List<Map> sections, List<Map> events) {
        if (null != sections && !sections.isEmpty()) {
            Map fieldMap = new HashMap();
            List<Map> fields = new ArrayList<Map>();
            List<Map> buttons = new ArrayList<Map>();
            for (Map section : sections) {
                if (section.get("regionType") != null) {
                    List<Map> cells = (List<Map>) section.get("cells");
                    if (cells != null && !cells.isEmpty()) {
                        if (section.get("regionType").toString().equals("EDIT") || section.get("regionType").toString().equals("FASTQUERY")
                                || section.get("regionType").toString().equals("ADVQUERY")
                                || section.get("regionType").toString().equals("DIGEST")) {
                            for (Map cell : cells) {
                                Map<String, Object> element = (Map<String, Object>) cell.get("element");// 获取element
                                if (null != element && !element.isEmpty()) {
                                    Map field = new HashMap();
                                    String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
                                    String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
                                    field.put("cellCode", cellCode);
                                    field.put("regionType", regionType);
                                    List<Map> cellEvents = ecSplitEvents(cell);
                                    field.put("events", cellEvents);
                                    if (regionType != null && regionType.equals("EDIT")) {
                                        List<Map> validates = ecSplitValidate(cell);
                                        field.put("validates", validates);
                                    }

                                    for (Entry<String, Object> entry : element.entrySet()) {
                                        field.put(entry.getKey(), entry.getValue());
                                    }
                                    fields.add(field);
                                    cell.remove("element");// 删除布局中的字段信息
                                }
                            }

                        } else if (section.get("regionType").toString().equals("LISTPT")
                                || section.get("regionType").toString().equals("DATAGRID")
                                || section.get("regionType").toString().equals("MNECODE")) {

                            for (Map<String, Object> cell : cells) {
                                Map field = new HashMap();
                                if (cell != null) {
                                    String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
                                    String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
                                    field.put("cellCode", cellCode);
                                    field.put("regionType", regionType);
                                    if (regionType != null && regionType.equals("DATAGRID")) {
                                        List<Map> cellEvents = ecSplitEvents(cell);
                                        field.put("events", cellEvents);
                                    }
                                    if (null != cell && !cell.isEmpty()) {
                                        for (Iterator<String> it = cell.keySet().iterator(); it.hasNext(); ) {
                                            String key = it.next();
                                            field.put(key, cell.get(key));
                                            if (!(key.equals("regionType") || key.equals("cellCode"))) {
                                                it.remove();
                                                cell.remove(key);
                                            }
                                        }
                                    }
                                    fields.add(field);
                                }
                            }

                        } else if (section.get("regionType").toString().equals("BUTTON")) {

                            for (Map<String, Object> cell : cells) {
                                Map button = new HashMap();
                                if (cell != null) {
                                    String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
                                    String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
                                    if (regionType != null && regionType.equals("BUTTON")) {
                                        List<Map> cellEvents = ecSplitEvents(cell);
                                        button.put("events", cellEvents);
                                    }
                                    button.put("regionType", regionType);
                                    button.put("cellCode", cellCode);
                                    if (null != cell && !cell.isEmpty()) {
                                        for (Iterator<String> it = cell.keySet().iterator(); it.hasNext(); ) {
                                            String key = it.next();
                                            Object value = cell.get(key);
                                            if (key.equals("id")) {
                                                button.put("name", value);
                                            } else if (key.equalsIgnoreCase("operatetype")) {
                                                button.put("operateType", value);
                                            } else if (key.equalsIgnoreCase("viewselect")) {
                                                button.put("viewSelect", value);
                                            } else if (key.equalsIgnoreCase("isconfirm")) {
                                                button.put("isConfirm", value);
                                            } else if (key.equalsIgnoreCase("ishide")) {
                                                button.put("isHide", value);
                                            } else if (key.equalsIgnoreCase("confirmcontent")) {
                                                button.put("confirmContent", value);
                                            } else if (key.equalsIgnoreCase("buttonstyle")) {
                                                button.put("buttonStyle", value);
                                            } else if (key.equalsIgnoreCase("useInMore")) {
                                                button.put("isUseMore", value);
                                            } else if (key.equalsIgnoreCase("ispermission")) {
                                                button.put("isPermission", value);
                                            } else if (key.equalsIgnoreCase("iscallback")) {
                                                button.put("isCallback", value);
                                            } else if (key.equalsIgnoreCase("iscustomfunc")) {
                                                button.put("isCustomFunc", value);
                                            } else if (key.equalsIgnoreCase("operateurl")) {
                                                button.put("operateUrl", value);
                                            } else if (key.equalsIgnoreCase("regionType")) {
                                                button.put("regionType", value);
                                            } else if (key.equalsIgnoreCase("namekey")) {
                                                button.put("displayName", value);
                                            } else if (key.equalsIgnoreCase("cellCode")) {
                                                button.put("cellCode", value);
                                            } else if (key.equalsIgnoreCase("scriptCode")) {
                                                button.put("scriptCode", value);
                                            }
                                            if (!(key.equals("regionType") || key.equals("cellCode"))) {
                                                it.remove();
                                                cell.remove(key);
                                            }
                                        }
                                    }
                                    buttons.add(button);
                                }
                            }
                        }
                    }
                }
            }

            if (events != null && !events.isEmpty()) {
                fieldMap.put("events", events);
            }
            if (buttons != null && !buttons.isEmpty()) {
                fieldMap.put("buttons", buttons);
            }
            if (fields != null && !fields.isEmpty()) {
                fieldMap.put("fields", fields);
            }

            return SerializeUitls.serializeAsXml(fieldMap);
        }

        return null;
    }

    /**
     * 拆分字段事件
     *
     * @param cell
     * @return List<Map> Map为单个Event属性
     */
    @SuppressWarnings("rawtypes")
    private List<Map> ecSplitEvents(Map<String, Object> cell) {
        if (null != cell && !cell.isEmpty()) {
            List<Map> events = new ArrayList<Map>();
            Map<String, Object> event = null;
            if (null != cell.get("funcname")) {
                String funcname = cell.get("funcname").toString();// 函数名串
                if (null != cell.get("funcbody")) {
                    String funcbody = cell.get("funcbody").toString();// 函数体串
                    String funcbody_es5 = (String) cell.get("funcbody_es5");
                    String[] names = EventUtils.analysisFuncname(funcname);
                    String[] bodys = EventUtils.analysisFuncbody(funcbody);
                    String[] bodys_es5 = EventUtils.analysisFuncbody(funcbody_es5);
                    if (names.length == bodys.length) {
                        for (int i = 0; i < names.length; i++) {
                            event = new HashMap<String, Object>();
                            event.put("name", names[i]);
                            event.put("function", bodys[i]);
                            event.put("function_es5", null == funcbody_es5 ? null : bodys_es5[i]);
                            events.add(event);
                        }
                        cell.remove("funcname"); // 布局中除去事件
                        cell.remove("funcbody");
                    }
                }
            }

            if (null != cell.get("callbackname")) {
                String callbackname = cell.get("callbackname").toString();// 函数名串 回调函数
                if (null != cell.get("callbackbody")) {
                    if (!EventUtils.isCallBack(callbackname)) {// callback事件名称中如不包含"callback="则添加
                        callbackname = "callback=" + callbackname;
                    }
                    String callbackbody = cell.get("callbackbody").toString();// 函数体串
                    String callbackbody_es5 = (String) cell.get("callbackbody_es5");
                    event = new HashMap<String, Object>();
                    event.put("name", callbackname);
                    event.put("function", callbackbody);
                    event.put("function_es5", callbackbody_es5);
                    events.add(event);
                    cell.remove("callbackname");// 布局中除去事件
                    cell.remove("callbackbody");
                    cell.remove("callbackbody_es5");
                }
            }

            return events;
        }
        return null;
    }

    /**
     * 拆分cell中的验证信息
     *
     * @param cell
     * @return List<Map> Map为单个Validate属性
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map> ecSplitValidate(Map<String, Object> cell) {
        if (null != cell && !cell.isEmpty()) {
            List<Map> validates = new ArrayList<Map>();
            if (cell.get("validate") != null && cell.get("validate") instanceof List) {
                validates = (List<Map>) cell.get("validate");
                cell.remove("validate");// 除去布局中的验证信息
            }
            return validates;
        }
        return null;
    }

    /**
     * 更改配置文件的结构 并抽取字段属性信息
     *
     * @param originalConfig 原始配置信息
     * @return Map<String, Object>
     * {"config":页面布局信息,"fieldConfig":字段属性信息}
     */
    @SuppressWarnings("unchecked")
    public String modifyConfigStructure(String originalConfig) {
        if (originalConfig != null && originalConfig.length() > 0) {
            timestamp = System.currentTimeMillis();
            random_num = Math.round(Math.random() * 10000);
            Map<String, Object> originalMap = (Map<String, Object>) SerializeUitls.deserialize(originalConfig);
            if (null != originalMap && !originalMap.isEmpty() && originalMap.get("layout") == null) {// 排除已处理过的视图
                Map<String, Object> configMap = new HashMap<String, Object>();// 改变层级结构后的config
                // layout
                Map<String, Object> layout = new HashMap<String, Object>();
                layout.put("layoutCode", DateUtils.getEcConfigNodeCode("layout").get("layout"));
                Map<String, Object> pageConfig = (Map<String, Object>) originalMap.get("pageConfig");// 页面信息
                if (pageConfig != null && !pageConfig.isEmpty()) {
                    layout.put("pageConfig", pageConfig);
                }
                Map<String, Object> listProperty = (Map<String, Object>) originalMap.get("listProperty");
                if (listProperty != null && !listProperty.isEmpty()) {
                    layout.put("listProperty", listProperty);
                }

                // 编辑视图
                // tabs
                List<Map<String, Object>> originalTabs = (List<Map<String, Object>>) originalMap.get("tabs");// 原始页签
                if (originalTabs != null && !originalTabs.isEmpty()) {
                    List<Map<String, Object>> tabs = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> orgTab : originalTabs) {
                        Map<String, Object> tab = new HashMap<String, Object>();
                        if (orgTab.get("name") != null) {// 页签信息
                            tab.put("name", orgTab.get("name"));
                            tab.put("namekey", (orgTab.get("namekey") == null) ? orgTab.get("name") : orgTab.get("namekey"));
                            tab.put("tabCode", DateUtils.getEcConfigNodeCode("tab").get("tab"));
                        }

                        // sections
                        List<Map<String, Object>> originalSections = (List<Map<String, Object>>) orgTab.get("sections");// 原始节信息
                        if (null != originalSections && !originalSections.isEmpty()) {
                            List<Map<String, Object>> sections = new ArrayList<Map<String, Object>>();
                            for (Map<String, Object> orgSection : originalSections) {
                                Map<String, Object> section = new HashMap<String, Object>();
                                section.put("sectionCode", DateUtils.getEcConfigNodeCode("section").get("section"));
                                section.put("regionType", "EDIT");
                                for (Entry<String, Object> entry : orgSection.entrySet()) {
                                    if (entry.getKey().equals("content")) {
                                        Map<String, Object> content = (Map<String, Object>) entry.getValue();
                                        if (content != null && !content.isEmpty()) {
                                            List<Map<String, Object>> forms = (List<Map<String, Object>>) content.get("form");
                                            if (null != forms && !forms.isEmpty()) {
                                                List<Map<String, Object>> cells = new ArrayList<Map<String, Object>>();
                                                for (Map<String, Object> form : forms) {
                                                    Map<String, Object> cell = new HashMap<String, Object>();
                                                    cell.put("cellCode", DateUtils.getEcConfigNodeCode("cell", timestamp, random_num++));
                                                    cell.put("regionType", "EDIT");
                                                    for (Entry<String, Object> entry2 : form.entrySet()) {
                                                        if (entry2.getKey().equalsIgnoreCase("element")) {
                                                            Map<String, Object> element = (Map<String, Object>) entry2.getValue();
                                                            if (!element.containsKey("key")) {
                                                                String key = "complex_";
                                                                String showType = "";
                                                                if (element.containsKey("showType")) {
                                                                    showType = element.get("showType").toString();
                                                                } else if (element.containsKey("fieldType")) {
                                                                    showType = element.get("fieldType").toString();
                                                                }
                                                                if (showType.equalsIgnoreCase("label")) {
                                                                    key = key + showType.toLowerCase() + "_" + System.currentTimeMillis()
                                                                            + "_" + Math.round(Math.random() * 10000);
                                                                } else if (element.containsKey("name") && element.get("name") != null) {
                                                                    key = key + element.get("name").toString();
                                                                }
                                                                element.put("key", key.replaceAll("\\.", "_"));
                                                            }
                                                        }
                                                        cell.put(entry2.getKey(), entry2.getValue());
                                                    }
                                                    cells.add(cell);
                                                }
                                                section.put("cells", cells);
                                            }
                                        }
                                    } else {
                                        section.put(entry.getKey(), entry.getValue());
                                    }
                                }
                                sections.add(section);// 节 list
                            }
                            tab.put("sections", sections); // 页签中添加section
                        }
                        tabs.add(tab);
                    }

                    layout.put("tabs", tabs);
                }

                // 列表视图 DataGrid
                Boolean isDataGrid = true;
                List<Map<String, Object>> orgButtons = (List<Map<String, Object>>) originalMap.get("operateButtons");
                List<Map<String, Object>> orgColumns = (List<Map<String, Object>>) originalMap.get("columns");
                Map<String, Object> orgMnes = (Map<String, Object>) originalMap.get("mnecodeset");// 助记码
                List<Map<String, Object>> orgFastSections = null;
                if (originalMap.get("fastsections") != null) {
                    orgFastSections = (List<Map<String, Object>>) originalMap.get("fastsections");
                    isDataGrid = false;
                }

                List<Map<String, Object>> sections = new ArrayList<Map<String, Object>>();
                // 按钮
                if (orgButtons != null && !orgButtons.isEmpty()) {
                    Map<String, Object> section = new HashMap<String, Object>();
                    section.put("sectionCode", DateUtils.getEcConfigNodeCode("section").get("section"));
                    section.put("regionType", "BUTTON");
                    List<Map<String, Object>> cells = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> button : orgButtons) {
                        Map<String, Object> cell = new HashMap<String, Object>();
                        for (Entry<String, Object> entry : button.entrySet()) {
                            cell.put("cellCode", DateUtils.getEcConfigNodeCode("cell", timestamp, random_num++));
                            cell.put("regionType", "BUTTON");
                            cell.put(entry.getKey(), entry.getValue());
                        }
                        cells.add(cell);
                    }
                    section.put("cells", cells);
                    sections.add(section);
                }

                // 列表PT DataGrid
                if (orgColumns != null && !orgColumns.isEmpty()) {
                    Map<String, Object> section = new HashMap<String, Object>();
                    section.put("sectionCode", DateUtils.getEcConfigNodeCode("section").get("section"));
                    if (isDataGrid) {
                        section.put("regionType", "DATAGRID");
                    } else {
                        section.put("regionType", "LISTPT");
                        if (listProperty != null && !listProperty.isEmpty()) {
                            section.put("listProperty", listProperty);
                            layout.remove("listProperty");
                        }
                    }
                    List<Map<String, Object>> cells = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> column : orgColumns) {
                        Map<String, Object> cell = new HashMap<String, Object>();
                        for (Entry<String, Object> entry : column.entrySet()) {
                            cell.put("cellCode", DateUtils.getEcConfigNodeCode("cell", timestamp, random_num++));
                            if (isDataGrid) {
                                cell.put("regionType", "DATAGRID");
                            } else {
                                cell.put("regionType", "LISTPT");
                            }
                            cell.put(entry.getKey(), entry.getValue());
                        }
                        cells.add(cell);
                    }
                    section.put("cells", cells);
                    if (orgMnes != null && !orgMnes.isEmpty()) {
                        section.put("mnecodeset", orgMnes);
                    }
                    sections.add(section);
                }

                // fastquery
                if (orgFastSections != null && !orgFastSections.isEmpty()) {
                    Map<String, Object> section = new HashMap<String, Object>();
                    section.put("sectionCode", DateUtils.getEcConfigNodeCode("section").get("section"));
                    section.put("regionType", "FASTQUERY");
                    List<Map<String, Object>> cells = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> fs : orgFastSections) {
                        for (Entry<String, Object> entry : fs.entrySet()) {
                            if (!entry.getKey().equals("content")) {
                                section.put(entry.getKey(), entry.getValue());
                            } else {
                                Map<String, Object> content = (Map<String, Object>) entry.getValue();
                                if (content != null && !content.isEmpty()) {
                                    List<Map<String, Object>> forms = (List<Map<String, Object>>) content.get("form");
                                    if (null != forms && !forms.isEmpty()) {
                                        for (Map<String, Object> form : forms) {
                                            Map<String, Object> cell = new HashMap<String, Object>();
                                            cell.put("cellCode", DateUtils.getEcConfigNodeCode("cell", timestamp, random_num++));
                                            cell.put("regionType", "FASTQUERY");
                                            for (Entry<String, Object> entry2 : form.entrySet()) {
                                                cell.put(entry2.getKey(), entry2.getValue());
                                            }
                                            cells.add(cell);
                                        }
                                        section.put("cells", cells);
                                    }
                                }
                            }
                        }
                    }
                    section.put("cells", cells);
                    sections.add(section);
                }
                if (sections != null && !sections.isEmpty()) {
                    layout.put("sections", sections);
                }
                configMap.put("layout", layout);
                return SerializeUitls.serializeAsXml(configMap);
            }
        }
        return null;
    }


    /**
     * 将视图的完整字段信息一次性组织为MAP
     *
     * @param infoMap
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFullFieldInfoMap(Map<String, List<?>> infoMap) {
        fieldEventMap = new HashMap<String, Object>();
        buttonEventMap = new HashMap<String, Object>();
        validateMap = new HashMap<String, Object>();
        fastQueryJsonMap = new HashMap<String, Object>();
        advQueryJsonMap = new HashMap<String, Object>();

        if (infoMap != null && !infoMap.isEmpty()) {
            List<Event> events = (List<Event>) infoMap.get("events");// 外部事件
            List<Field> fields = (List<Field>) infoMap.get("fields");// cells 下字段
            List<Button> buttons = (List<Button>) infoMap.get("buttons");// 按钮
            List<FastQueryJson> fastQueryJsons = (List<FastQueryJson>) infoMap.get("fastQueryJsons");
            List<AdvQueryJson> advQueryJsons = (List<AdvQueryJson>) infoMap.get("advQueryJsons");

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append("<fullFieldConfig>");
            sb.append("<cells>");
            sb.append("<list>");
            if (fields != null && !fields.isEmpty()) {
                for (Field field : fields) {
                    sb.append("<list-item>");
                    sb.append("<cellCode><![CDATA[" + field.getCellCode() + "]]></cellCode>");
                    if (field.getConfig() != null && field.getConfig().length() > 0) {
                        String fieldXml = XmlUtils.getPartConfig(field.getConfig());
                        // 把ecEnv插入xml中
                        fieldXml = fieldXml.substring(0, fieldXml.length() - 8) + "<ecEnv>" + field.getEcEnv() + "</ecEnv></field>";
                        sb.append(fieldXml);
                    } else {
                        sb.append("<field></field>");
                    }
                    Set<Event> eventSet = eventService.getByFieldCode(field.getCode());
                    if (eventSet != null) {
                        fieldEventMap.put(field.getCellCode(), eventSet);
                    }
                    Set<Validate> validateSet = validateService.getByFieldCode(field.getCode());
                    if (validateSet != null) {
                        validateMap.put(field.getCellCode(), validateSet);
                    }
                    sb.append("</list-item>");
                }
            }
            if (buttons != null && !buttons.isEmpty()) {
                for (Button button : buttons) {
                    if (StringUtils.isEmpty(button.getCellCode())) {
                        continue;
                    }
                    Set<Event> eventSet = eventService.getByButtonCode(button.getCode());
                    if (eventSet != null) {
                        buttonEventMap.put(button.getCellCode(), eventSet);
                    }
                    sb.append("<list-item>");
                    sb.append("<cellCode><![CDATA[" + button.getCellCode() + "]]></cellCode>");
                    sb.append("<button>");
                    if (null != button.getName() && button.getName().length() > 0) {
                        sb.append("<id><![CDATA[" + button.getName() + "]]></id>");
                    }
                    if (null != button.getDisplayName() && button.getDisplayName().length() > 0) {
                        sb.append("<namekey><![CDATA[" + button.getDisplayName() + "]]></namekey>");
                    }
                    if (null != button.getButtonStyle() && button.getButtonStyle().length() > 0) {
                        sb.append("<buttonstyle><![CDATA[" + button.getButtonStyle() + "]]></buttonstyle>");
                    }
                    if (null != button.getButtonOperationCode() && button.getButtonOperationCode().length() > 0) {
                        sb.append("<buttonoperationcode><![CDATA[" + button.getButtonOperationCode() + "]]></buttonoperationcode>");
                    }
                    if (null != button.getIsUseMore()) {
                        sb.append("<useInMore><![CDATA[" + button.getIsUseMore() + "]]></useInMore>");
                    }
                    if (null != button.getOperateType()) {
                        sb.append("<operatetype><![CDATA[" + button.getOperateType() + "]]></operatetype>");
                    }
                    if (StringUtils.isNotBlank(button.getViewSelectCode())) {
                        sb.append("<viewselect><![CDATA[" + button.getViewSelectCode() + "]]></viewselect>");
                    }
                    if (null != button.getOperateType() && button.getOperateType().name().equals("REF")) {
                        for (Field f : fields) {
                            if (f.getCode().contains(button.getReleaseFelid() != null ? button.getReleaseFelid() : "")
                                    && (f.getShowFormat() != null
                                    && ShowFormat.SELECTCOMP.name().equals(f.getShowFormat().name()))) {
                                String dataGridCode = f.getDataGridCode();
                                DataGrid dataGrid = dataGridService.getById(dataGridCode);
                                sb.append("<objName><![CDATA[" + dataGrid.getName() + f.getKey() + "]]></objName>");
                            }
                        }
                        String viewSelectCode = button.getViewSelectCode();
                        View view = viewServiceFoundation.getView(viewSelectCode);
                        sb.append("<viewurl><![CDATA[" +view.getUrl() + "]]></viewurl>");
                        sb.append("<viewname><![CDATA[" + view.getTitle() + "]]></viewname>");
                    }
                    if (null != button.getIsCallback()) {
                        sb.append("<iscallback><![CDATA[" + button.getIsCallback() + "]]></iscallback>");
                    }
                    if (null != button.getIsCustomFunc()) {
                        sb.append("<iscustomfunc><![CDATA[" + button.getIsCustomFunc() + "]]></iscustomfunc>");
                    }
                    if (null != button.getIsPermission()) {
                        sb.append("<ispermission><![CDATA[" + button.getIsPermission() + "]]></ispermission>");
                    }
                    if (null != button.getIsConfirm()) {
                        sb.append("<isconfirm><![CDATA[" + button.getIsConfirm() + "]]></isconfirm>");
                    }
                    if (null != button.getIsHide()) {
                        sb.append("<isHide><![CDATA[" + button.getIsHide() + "]]></isHide>");
                    }
                    if (null != button.getOperateUrl() && button.getOperateUrl().length() > 0) {
                        sb.append("<operateurl><![CDATA[" + button.getOperateUrl() + "]]></operateurl>");
                    }
                    if (null != button.getConfirmContent() && button.getConfirmContent().length() > 0) {
                        sb.append("<confirmcontent><![CDATA[" + button.getConfirmContent() + "]]></confirmcontent>");
                    }
                    if (null != button.getScriptCode() && button.getScriptCode().length() > 0) {
                        sb.append("<scriptCode><![CDATA[" + button.getScriptCode() + "]]></scriptCode>");
                    }
                    if (null != button.getEcEnv()) {
                        sb.append("<ecEnv><![CDATA[" + button.getEcEnv() + "]]></ecEnv>");
                    }
                    if (null != button.getPermissionCode()) {
                        sb.append("<permissionCode><![CDATA[" + button.getPermissionCode() + "]]></permissionCode>");
                    }
                    if (null != button.getButtonAlign()) {
                        sb.append("<buttonAlign><![CDATA[" + button.getButtonAlign() + "]]></buttonAlign>");
                    }
                    if (null != button.getIsPublished()) {
                        sb.append("<isPublished><![CDATA[" + button.getIsPublished() + "]]></isPublished>");
                    }
                    if (null != button.getIsSignatureConfig()) {
                        sb.append("<isSignatureConfig><![CDATA[" + button.getIsSignatureConfig() + "]]></isSignatureConfig>");
                    }
                    if (null != button.getReleaseFelid()) {
                        sb.append("<releaseFelid><![CDATA[" + button.getReleaseFelid() + "]]></releaseFelid>");
                    }
                    if (null != button.getConfigMap().get("button")) {
                        if (button.getConfigMap().get("button").toString().indexOf("permissionFromCode") != -1) {
                            sb.append("<permissionFromCode><![CDATA[" + button.getConfigMap().get("button").toString().substring(button.getConfigMap().get("button").toString().indexOf("=", button.getConfigMap().get("button").toString().indexOf("permissionFromCode")) + 1, button.getConfigMap().get("button").toString().indexOf(",", button.getConfigMap().get("button").toString().indexOf("permissionFromCode"))) + "]]></permissionFromCode>");
                        }
                    }
                    if (null != button.getConfigMap().get("button")) {
                        if (button.getConfigMap().get("button").toString().indexOf("permissionFromName") != -1) {
                            sb.append("<permissionFromName><![CDATA[" + button.getConfigMap().get("button").toString().substring(button.getConfigMap().get("button").toString().indexOf("=", button.getConfigMap().get("button").toString().indexOf("permissionFromName")) + 1, button.getConfigMap().get("button").toString().indexOf(",", button.getConfigMap().get("button").toString().indexOf("permissionFromName"))) + "]]></permissionFromName>");
                        }
                    }
                    sb.append("</button>");
                    sb.append("</list-item>");
                }
            }
            sb.append("</list>");
            sb.append("</cells>");
            sb.append("<events>");
            if (events != null && !events.isEmpty()) {
                sb.append("<list>");
                for (Event event : events) {
                    sb.append("<list-item>");
                    if (event.getLayoutCode() != null && event.getLayoutCode().length() > 0) {
                        sb.append("<layoutCode><![CDATA[" + event.getLayoutCode() + "]]></layoutCode>");
                    } else if (event.getSectionCode() != null && event.getSectionCode().length() > 0) {
                        sb.append("<sectionCode><![CDATA[" + event.getSectionCode() + "]]></sectionCode>");
                    } else if (event.getTabCode() != null && event.getTabCode().length() > 0) {
                        sb.append("<tabCode><![CDATA[" + event.getTabCode() + "]]></tabCode>");
                    }
                    if (event.getName() != null && event.getName().length() > 0) {
                        sb.append("<name><![CDATA[" + event.getName() + "]]></name>");
                    }
                    if (event.getFunction() != null && event.getFunction().length() > 0) {
                        sb.append("<function><![CDATA[" + event.getFunction() + "]]></function>");
                    }
                    if (event.getFunction_es5() != null && event.getFunction_es5().length() > 0) {
                        sb.append("<function_es5><![CDATA[" + event.getFunction_es5() + "]]></function_es5>");
                    }
                    sb.append("</list-item>");
                }
                sb.append("</list>");
            }
            sb.append("</events>");
            sb.append("<fastQueryJsons>");
            if (fastQueryJsons != null && !fastQueryJsons.isEmpty()) {
                sb.append("<list>");
                for (FastQueryJson fastQueryJson : fastQueryJsons) {
                    sb.append("<list-item>");
                    if (fastQueryJson.getCode() != null && fastQueryJson.getCode().length() > 0) {
                        sb.append("<code><![CDATA[" + fastQueryJson.getCode() + "]]></code>");
                    }
                    if (fastQueryJson.getLayoutName() != null && fastQueryJson.getLayoutName().length() > 0) {
                        sb.append("<layoutName><![CDATA[" + fastQueryJson.getLayoutName() + "]]></layoutName>");
                    }
                    sb.append("</list-item>");
                }
                sb.append("</list>");
            }
            sb.append("</fastQueryJsons>");
            sb.append("<advQueryJsons>");
            if (advQueryJsons != null && !advQueryJsons.isEmpty()) {
                sb.append("<list>");
                for (AdvQueryJson advQueryJson : advQueryJsons) {
                    sb.append("<list-item>");
                    if (advQueryJson.getCode() != null && advQueryJson.getCode().length() > 0) {
                        sb.append("<code><![CDATA[" + advQueryJson.getCode() + "]]></code>");
                    }
                    if (advQueryJson.getLayoutName() != null && advQueryJson.getCode().length() > 0) {
                        sb.append("<layoutName><![CDATA[" + advQueryJson.getLayoutName() + "]]></layoutName>");
                    }
                    sb.append("</list-item>");
                }
                sb.append("</list>");
            }
            sb.append("</advQueryJsons>");
            sb.append("</fullFieldConfig>");
            // LOGGER.info("==FIELD FULL CONFIG==||" + sb.toString());
            return (Map<String, Object>) SerializeUitls.deserialize(sb.toString());
        }
        return null;
    }

    /**
     * 组织完整Config
     *
     * @param configMap
     * @param fullFieldMap
     * @param isExtra      是否是增强型视图
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getFullConfig(Map<String, Object> configMap, Map<String, Object> fullFieldMap, boolean isExtra) {
        if (configMap != null && !configMap.isEmpty()) {
            if (fullFieldMap != null && !fullFieldMap.isEmpty()) {
                Map<String, Object> layout = (Map<String, Object>) configMap.get("layout");// layout
                if (layout != null) {
                    Map<String, Object> pageConfig = (Map<String, Object>) layout.get("pageConfig");// 页面信息
                    if (!layout.isEmpty()) {
                        if (null != layout.get("layoutCode")) {        //解决增强型视图存在layout但是没有layoutcode 的bug
                            String layoutCode = layout.get("layoutCode").toString();
                            List<Map<String, Object>> events = (List<Map<String, Object>>) fullFieldMap.get("events");
                            if (events != null && !events.isEmpty()) {
                                // 页面事件
                                for (Map<String, Object> event : events) {
                                    if (event.get("layoutCode") != null && event.get("layoutCode").toString().length() > 0
                                            && event.get("layoutCode").toString().equals(layoutCode)) {
                                        pageConfig.put(event.get("name").toString(), (event.get("function") == null) ? "" : event.get("function")
                                                .toString());
                                        pageConfig.put(event.get("name").toString() + "_es5", (event.get("function_es5") == null) ? "" : event.get("function_es5")
                                                .toString());
                                    }
                                }
                            }
                        }
                    }

                    List<Map<String, Object>> fullCells = (List<Map<String, Object>>) fullFieldMap.get("cells");
                    List<Map<String, Object>> fullFastQueryJsons = new ArrayList<Map<String, Object>>();
                    List<Map<String, Object>> fullAdvQueryJsons = new ArrayList<Map<String, Object>>();
                    if (fullFieldMap.get("fastQueryJsons") != null) {
                        fullFastQueryJsons = (List<Map<String, Object>>) fullFieldMap.get("fastQueryJsons");
                    }
                    if (fullFieldMap.get("advQueryJsons") != null) {
                        fullAdvQueryJsons = (List<Map<String, Object>>) fullFieldMap.get("advQueryJsons");
                    }
                    // layout-->tabs-->section
                    List<Map<String, Object>> tabs = (List<Map<String, Object>>) layout.get("tabs");// tabs页签
                    if (tabs != null && !tabs.isEmpty()) {
                        for (Map<String, Object> tab : tabs) {
                            if (isExtra) {
                                getFullConfigTransfer(tab, fullCells, fullFastQueryJsons, fullAdvQueryJsons);
                            } else {
                                // tab内的sections
                                List<Map<String, Object>> sections = (List<Map<String, Object>>) tab.get("sections");// tab
                                ecSectionConfigBuilder(sections, fullCells);
                            }
                        }
                    }

                    // layout中的section
                    List<Map<String, Object>> layoutSections = (List<Map<String, Object>>) layout.get("sections");// layout
                    // sections
                    if (layoutSections != null && !layoutSections.isEmpty()) {
                        ecSectionConfigBuilder(layoutSections, fullCells);
                    }
                }
            }
            return SerializeUitls.serializeAsXml(configMap);
        }
        return null;
    }

    /**
     * 增强型布局 布局信息与字段信息整合 遍历器 to 增强型布局
     */
    @SuppressWarnings("unckecked")
    public void getFullConfigTransfer(Map<String, Object> sections, List<Map<String, Object>> fullCells, List<Map<String, Object>> fullFastQueryJsons, List<Map<String, Object>> fullAdvQueryJsons) {
        Map<String, Object> layoutProperties = (Map<String, Object>) sections.get("layoutProperties");
        if (null != sections.get("layout")) {
            if (null != sections.get("layoutProperties")) {
                List<Map<String, Object>> layoutList = (List<Map<String, Object>>) sections.get("layout");
                for (Map<String, Object> layouts : layoutList) {

                    if (null != layoutProperties && null != layoutProperties.get("layoutmethod")) {
                        if ((layoutProperties.get("layoutmethod").toString().equals("row"))
                                || (layoutProperties.get("layoutmethod").toString().equals("column"))
                                || (layoutProperties.get("layoutmethod").toString().equals("tab"))) {     // 横向、纵向、页签布局
                            getFullConfigTransfer(layouts, fullCells, fullFastQueryJsons, fullAdvQueryJsons);
                        } else if ((layoutProperties.get("layoutmethod").toString().equals("container"))) {
                            List<Map<String, Object>> layoutsSection = (List<Map<String, Object>>) layouts.get("sections");
                            ecSectionConfigBuilder(layoutsSection, fullCells);
                        }
                    }
                }
            }
        } else if (null != sections.get("tabs")) {        //页签布局
            if (null != sections.get("layoutProperties")) {
                List<Map<String, Object>> tabs = (List<Map<String, Object>>) sections.get("tabs");
                for (Map<String, Object> tab : tabs) {
                    getFullConfigTransfer(tab, fullCells, fullFastQueryJsons, fullAdvQueryJsons);
                }

                tabs = null;
            }
        } else if (null != sections.get("sections")) {        //内容
            if (layoutProperties.get("layoutmethod").toString().equals("container")) {
                ecSectionConfigBuilder((List<Map<String, Object>>) sections.get("sections"), fullCells);
            }

            layoutProperties = null;
        }
    }

    @SuppressWarnings("unchecked")
    public String getExtraFastQueryConfig(Map<String, Object> configMap, Map<String, Object> fullFieldMap) {
        if (null != configMap && !configMap.isEmpty()) {
            if (null != fullFieldMap && !fullFieldMap.isEmpty()) {
                Map<String, Object> fastQueryJson = (Map<String, Object>) configMap.get("fastQueryJson");// fastQueryJson
                List<Map<String, Object>> sections = (List<Map<String, Object>>) fastQueryJson.get("sections");// 页面信息
                List<Map<String, Object>> fullCells = (List<Map<String, Object>>) fullFieldMap.get("cells");
                if (null != sections && !sections.isEmpty()) {
                    ecSectionConfigBuilder(sections, fullCells);
                }
            }
            return SerializeUitls.serializeAsXml(configMap);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String getExtraAdvQueryConfig(Map<String, Object> configMap, Map<String, Object> fullFieldMap) {
        if (null != configMap && !configMap.isEmpty()) {
            if (null != fullFieldMap && !fullFieldMap.isEmpty()) {
                Map<String, Object> advQueryJson = (Map<String, Object>) configMap.get("advQueryJson");// advQueryJson
                List<Map<String, Object>> sections = (List<Map<String, Object>>) advQueryJson.get("sections");// 页面信息
                List<Map<String, Object>> fullCells = (List<Map<String, Object>>) fullFieldMap.get("cells");
                if (null != sections && !sections.isEmpty()) {
                    ecSectionConfigBuilder(sections, fullCells);
                }
            }
            return SerializeUitls.serializeAsXml(configMap);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void ecSectionConfigBuilder(List<Map<String, Object>> sections, List<Map<String, Object>> fullCells) {
        if (sections != null && !sections.isEmpty() && fullCells != null && !fullCells.isEmpty()) {
            for (Map<String, Object> section : sections) {
                String regionType = "";
                if (section.get("regionType") != null && section.get("regionType").toString().length() > 0) {
                    regionType = section.get("regionType").toString();
                }
                List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
                if (cells != null && !cells.isEmpty()) {
                    if (regionType.equals("EDIT") || regionType.equals("FASTQUERY") || regionType.equals("ADVQUERY") || regionType.equals("LISTPT")
                            || regionType.equals("DATAGRID") || regionType.equals("MNECODE") || regionType.equals("BUTTON") || regionType.equals("DIGEST")) {
                        List<Map<String, Object>> removeCells = new ArrayList<Map<String, Object>>();
                        for (Map<String, Object> cell : cells) {
                            boolean fieldExist = false;
                            Object cellCode = cell.get("cellCode");
                            for (Map<String, Object> cell0 : fullCells) {
                                Object cellCode0 = cell0.get("cellCode");
                                if (cellCode != null && cellCode.toString().length() > 0 && cellCode0 != null
                                        && cellCode0.toString().length() > 0 && cellCode.toString().equals(cellCode0.toString())) {
                                    fieldExist = true;
                                    if (regionType.equals("EDIT") || regionType.equals("FASTQUERY") || regionType.equals("ADVQUERY") || regionType.equals("DATAGRID") || regionType.equals("DIGEST")) {
                                        if (fieldEventMap != null && !fieldEventMap.isEmpty()) {
                                            Set<Event> eventSet = (Set<Event>) fieldEventMap.get(cellCode0.toString());
                                            if (eventSet != null && !eventSet.isEmpty()) {
                                                ecCellEventBuilder(cell, eventSet);
                                            }
                                        }
                                        if (validateMap != null && !validateMap.isEmpty()) {
                                            Set<Validate> validateSet = (Set<Validate>) validateMap.get(cellCode0.toString());
                                            if (validateSet != null && !validateSet.isEmpty()) {
                                                ecCellValidateBuilder(cell, validateSet);
                                            }
                                        }
                                    } else if (regionType.equals("BUTTON")) {
                                        if (buttonEventMap != null && !buttonEventMap.isEmpty()) {
                                            Set<Event> eventSet = (Set<Event>) buttonEventMap.get(cellCode0.toString());
                                            if (eventSet != null && !eventSet.isEmpty()) {
                                                ecCellEventBuilder(cell, eventSet);
                                            }
                                        }
                                    }
                                    Map<String, Object> attrMap = null;
                                    if (!regionType.equals("BUTTON")) {
                                        attrMap = (Map<String, Object>) cell0.get("field");
                                    } else if (regionType.equals("BUTTON")) {
                                        attrMap = (Map<String, Object>) cell0.get("button");
                                    }
                                    if (attrMap != null && !attrMap.isEmpty()) {
                                        attrMap.remove("events");
                                        attrMap.remove("validates");
                                        attrMap.remove("cellCode");
                                        attrMap.remove("regionType");
                                        if (regionType.equals("EDIT") || regionType.equals("DIGEST")) {
                                            cell.put("element", attrMap);
                                        } else if (regionType.equals("LISTPT") || regionType.equals("DATAGRID")
                                                || regionType.equals("MNECODE") || regionType.equals("BUTTON")) {
                                            for (Entry<String, Object> entry : attrMap.entrySet()) {
                                                cell.put(entry.getKey(), entry.getValue());
                                            }
                                        } else if (regionType.equals("FASTQUERY") || regionType.equals("ADVQUERY")) {
                                            if (cell.get("element") != null) {
                                                Map<String, Object> cellElement = (Map<String, Object>) cell.get("element");
                                                for (Entry<String, Object> entry : attrMap.entrySet()) {
                                                    cellElement.put(entry.getKey(), entry.getValue());
                                                }
                                                cell.put("element", cellElement);
                                            } else {
                                                cell.put("element", attrMap);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }

                            if (!fieldExist && (regionType.equals("LISTPT") || regionType.equals("DATAGRID"))) {
                                removeCells.add(cell); // 加入空白cell
                            }
                        }
                        if (removeCells != null && !removeCells.isEmpty()) {
                            cells.removeAll(removeCells); // 删除空白cell
                        }
                    }
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
		String fieldPath = "E:\\bapWorkSpace\\bap-2.4.x\\TestCommon\\edit1.xml";
		File file = new File(fieldPath);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String s = null;
		StringBuffer sb = new StringBuffer();
		while ((s = reader.readLine()) != null) {
			sb.append(s + "\n");
		}
		reader.close();
        EcExtraViewIntegrationUtils ecExtraViewIntegrationUtils = new EcExtraViewIntegrationUtils();
        String config = ecExtraViewIntegrationUtils.modifyConfigStructure(sb.toString());
		System.out.println("full:" + config);
		Map<String, Object> map = ecExtraViewIntegrationUtils.ecSplitConfig(config);
		String fieldConfig = map.get("fieldConfig").toString();
		System.out.println("config:" + map.get("config").toString());
		System.out.println("fieldConfig:" + fieldConfig);

    }
}
