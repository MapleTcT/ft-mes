/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * <p>
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.entities.RolePSpecialPermission;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.entities.UserPSpecialPermission;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.vo.SpecialPermissionVO;
import com.supcon.supfusion.configuration.services.openapi.wrapper.SpecialPermissionWrapper;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.*;


/**
 * 特殊权限
 * @author zhangbobin
 * @date 2015年10月12日
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1/ec/scheduler")
public class SpecialPermissionController extends ConfigurationBaseController {

    private static final SpecialPermissionWrapper specialPermissionWrapper = new SpecialPermissionWrapper();
    @Autowired
    private MenuOperateService menuOperateService;

    @Resource
    private ViewService viewService;

    @Resource
    private ModelService modelService;

    @Autowired
    private SpecialPermissionService specialPermissionService;

    @Autowired
    private RoleUserService roleUserService;

    @Autowired
    private SystemCodeService systemCodeService;

    @Autowired
    private SpecialPermissionForUShowService specialPermissionForUShowService;

    @Autowired
    private SpecialPermissionForRShowService specialPermissionForRShowService;

    @Autowired
    private UserPSpecialPermissionService userPSpecialPermissionService;

    @Autowired
    private RolePSpecialPermissionService rolePSpecialPermissionService;

    /**
     * 特殊资源主窗口
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/ec/view/specialAuthConifg")
    public String otherConifgDefault(ModelMap map, Long operateId) throws SQLException {
        MenuOperate menuOperate = menuOperateService.load(operateId);
        if (menuOperate != null) {
            String viewCode = menuOperate.getMenuInfo().getCode();
            View ec_view = viewService.getView(viewCode);
            View runtime_view = viewService.getView(viewCode);
            String modelCode = null;
            if (ec_view != null) {
                modelCode = ec_view.getAssModel().getCode();
            } else if (runtime_view != null) {//如果以包形式启动的模块数据存放在RUNTIME表
                modelCode = runtime_view.getAssModel().getCode();
            }
            map.addAttribute("menuOperate", menuOperate);
            map.addAttribute("modelCode", modelCode);
        }
        return "view/special-permission-config";
    }

    /**
     * 特殊资源右侧列表
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/ec/view/showSpecialPermissionList")
    public String showSpecialPermissionList(ModelMap map, String targetModelCode, String specialPermissionCode, Boolean isTree) throws SQLException {
        Property bussinessProperty = modelService.getBussinessKeyProperty(targetModelCode);
        Property mainDisplayProperty = modelService.getMainDisplayProperty(targetModelCode);
        Property pkProperty = modelService.getPKProperty(targetModelCode);
        //associateName=associateName.toUpperCase();
        //去掉点号带来的影响
        String formatSpecilPermissionCode = specialPermissionCode.replaceAll("\\.", "\\_");
        map.addAttribute("bussinessProperty", "bussinessProperty");
        map.addAttribute("mainDisplayProperty", "mainDisplayProperty");
        map.addAttribute("pkProperty", "pkProperty");
        map.addAttribute("formatSpecilPermissionCode", "formatSpecilPermissionCode");
        //是否树形
        if (isTree != null && isTree) {
            map.addAttribute("systemCodeType", "tree");
        }
        //查询系统编码实体
        if (specialPermissionCode != null) {
            SpecialPermission sp = specialPermissionService.load(specialPermissionCode);
            if (sp.getType().equals("SYSTEMCODE")) {
                Property pro = sp.getProperty();
                String systemEntityCode = specialPermissionService.getSystemEntityCodeByProperty(pro);
                Boolean isSeniorSystemCode = sp.getProperty().getSeniorSystemCode();
                map.addAttribute("systemEntityCode", "systemEntityCode");
                map.addAttribute("isSeniorSystemCode", "isSeniorSystemCode");
            }
        }
        return "view/special-permission-list";
    }


    /**
     * 特殊资源选择系统编码页面
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/ec/view/specialPermissionSystemcodeSel")
    public String specialPermissionSystemcodeSel() throws SQLException {
        return "view/special-permission-systemcodeSel";
    }


    /**
     * 特殊资源实体列表
     *
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/listChildren")
    public List<SpecialPermissionVO> listChildren(String modelCode) throws SQLException {
        List<SpecialPermission> specialAuthortyList = new ArrayList<SpecialPermission>();
        if (modelCode != null && modelCode.length() > 0) {
            specialAuthortyList = specialPermissionService.findAllSpecialPermissionByModelCode(modelCode);
            if (specialAuthortyList != null && specialAuthortyList.size() > 0) {
                for (SpecialPermission sa : specialAuthortyList) {
                    sa.setPropertyName(InternationalResource.get(sa.getProperty().getDisplayName()));
                    if (sa.getType().equals("OBJECT")) {
                        sa.setPropertyName(sa.getPropertyName() + " [" + sa.getTargetModelName() + "]");
                        sa.setAssociateName(sa.getProperty().getAssociatedProperty().getName());
                        sa.setAssociateType(sa.getProperty().getAssociatedProperty().getType().toString());
                        sa.setAssociateCode(sa.getProperty().getAssociatedProperty().getCode());
                    }
                    if (sa.getRefView() != null) {
                        sa.setRefViewUrl(sa.getRefView().getUrl());
                    }
                }
            }
        }
        return specialPermissionWrapper.e2vList(specialAuthortyList);
    }


    /**
     * 保存用户权限用于展示的实体
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/saveSpecialPermissionForUShow")
    public void saveUserShowInfo(Long userId, Long operateId, String specialPermissionCode, String data, boolean isAssigned) throws SQLException {
        specialPermissionForUShowService.deleteUserShowHistoryData(userId, operateId, specialPermissionCode);
        JSONArray dataJson = null;
        if (data != null && !data.equals("")) {
            try {
                dataJson = new JSONArray(data);
                if (dataJson != null && dataJson.length() > 0) {
                    for (int i = 0; i < dataJson.length(); i++) {
                        JSONObject o = dataJson.getJSONObject(i);
                        SpecialPermissionForUShow show = new SpecialPermissionForUShow();
                        show.setOperateId(o.getLong("operateId"));
                        show.setUserId(o.getLong("userId"));
                        show.setSpecialPermission(specialPermissionService.load(o.getString("specialPermissionCode")));
                        show.setValueId(o.getString("valueId"));
                        show.setValueTitle(o.getString("valueTitle"));
                        show.setLayRec(o.getString("layRec"));
                        show.setValueCode(o.getString("valueCode"));
                        show.setIsIncludeSub(o.getString("isIncludeSub"));
                        show.setIsAssigned(isAssigned);
                        specialPermissionForUShowService.save(show);

                    }
                }
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 保存角色权限用于展示的实体
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/saveSpecialPermissionForRShow")
    public void saveRoleShowInfo(Long roleId, Long operateId, String specialPermissionCode, String data, Boolean isAssigned) throws SQLException {
        specialPermissionForRShowService.deleteRoleShowHistoryData(roleId, operateId, specialPermissionCode);
        JSONArray dataJson = null;
        if (data != null && !data.equals("")) {
            try {
                dataJson = new JSONArray(data);
                if (dataJson != null && dataJson.length() > 0) {
                    for (int i = 0; i < dataJson.length(); i++) {
                        JSONObject o = dataJson.getJSONObject(i);
                        SpecialPermissionForRShow show = new SpecialPermissionForRShow();
                        show.setOperateId(o.getLong("operateId"));
                        show.setRoleId(o.getLong("roleId"));
                        show.setSpecialPermission(specialPermissionService.load(o.getString("specialPermissionCode")));
                        show.setValueId(o.getString("valueId"));
                        show.setValueTitle(o.getString("valueTitle"));
                        show.setValueCode(o.getString("valueCode"));
                        show.setLayRec(o.getString("layRec"));
                        show.setIsIncludeSub(o.getString("isIncludeSub"));
                        show.setIsAssigned(isAssigned);
                        specialPermissionForRShowService.save(show);

                    }
                }
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    /**
     * 得到该用户的所有的specialpermissionshow配置信息
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/showUShowInfo")
    public Page<Map<String, Object>> showUShowInfo(Integer pageNo, Integer pageSize, Boolean isFromRole, Long userId,
                                                   Long operateId, String specialPermissionCode, Boolean isAssigned,
                                                   Boolean isTree, String type) throws SQLException {
        Page<Map<String, Object>> records = new Page<Map<String, Object>>(pageNo, pageSize);
        // 根据queryConditionJson 生成SQL
        StringBuffer sqlSB = new StringBuffer();
        StringBuffer sqlPg = new StringBuffer();
        List<Object> parameters = new LinkedList<Object>();
        if (isFromRole != null && !isFromRole) {
            sqlSB.append("select  s.VALUE_ID ID,s.VALUE_TITLE as TITLE,s.VALUE_CODE as CODE,s.LAY_REC  LAYREC,s.VALUE_ID VALUE,s.IS_INCLUDE_SUB ISINCLUDESUB  from EC_SPECIAL_PERMISSION_USHOW s where s.valid=1  and  s.USER_Id=?  and  s.OPERATE_ID=?  and s.SPECIAL_PERMISSION_CODE=? and s.IS_ASSIGNED=?");
            sqlPg.append("select count(1) from EC_SPECIAL_PERMISSION_USHOW show where show.VALID=1  and  show.USER_Id=?  and  show.OPERATE_ID=?  and show.SPECIAL_PERMISSION_CODE=? and show.IS_ASSIGNED=?");
            parameters.add(userId);
            parameters.add(operateId);
            parameters.add(specialPermissionCode);
            parameters.add(isAssigned);
            Object[] params = new Object[parameters.size()];
            sqlSB.append(" order by s.ID ASC");
            records = specialPermissionForUShowService.findRecordPage(records, sqlSB.toString(), sqlPg.toString(), parameters.toArray(params));
            if (isTree != null && isTree && type != null && type.equals("SYSTEMCODE")) {
                List<Map<String, Object>> result = records.getResult();
                for (Map<String, Object> map : result) {
                    Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Object> entry = it.next();
                        if (entry.getKey() != null && entry.getKey().equals("TITLE")) {
                            Map<String, Object> t = new HashMap<String, Object>();
                            SystemCode sysCode = systemCodeService.load(entry.getValue().toString());
                            t.put("id", entry.getValue().toString());
                            if (sysCode != null) {
                                t.put("value", InternationalResource.get(sysCode.getValue()));
                            }
                            map.put("TITLE", t);
                        }
                    }
                }
            }
        } else if (isFromRole != null && isFromRole) {
            // 1.根据userId找到关联的roleIds
            // 2.根据roleId拿到关联的数据
            StringBuilder sysmbol = new StringBuilder();
            List<Long> roleIds = roleUserService.getRoleUserByUserId(userId);
            for (int i = 0; i < roleIds.size(); i++) {
                sysmbol.append(",").append("?");
            }
            if (sysmbol != null && sysmbol.length() > 0) {
                sysmbol.deleteCharAt(0);
            }
            sqlSB.append("select  s.VALUE_ID ID,s.VALUE_TITLE as TITLE,s.VALUE_CODE as CODE,s.LAY_REC LAYREC,s.IS_INCLUDE_SUB ISINCLUDESUB  from EC_SPECIAL_PERMISSION_RSHOW s where s.valid=1  and  s.ROLE_Id in (" + sysmbol.toString() + ")  and  s.OPERATE_ID=? and s.SPECIAL_PERMISSION_CODE=? and s.IS_ASSIGNED=?");
            sqlPg.append("select count(1) from EC_SPECIAL_PERMISSION_RSHOW show where show.VALID=1  and  show.ROLE_Id in (" + sysmbol.toString() + ") and  show.OPERATE_ID=?  and show.SPECIAL_PERMISSION_CODE=?  and show.IS_ASSIGNED=? ");
            for (int i = 0; i < roleIds.size(); i++) {
                parameters.add(roleIds.get(i));
            }
            parameters.add(operateId);
            parameters.add(specialPermissionCode);
            parameters.add(isAssigned);
            Object[] params = new Object[parameters.size()];
            sqlSB.append(" order by s.ID ASC");
            records = specialPermissionForUShowService.findRecordPage(records, sqlSB.toString(), sqlPg.toString(), parameters.toArray(params));
            if (isTree != null && isTree && type != null && type.equals("SYSTEMCODE")) {
                List<Map<String, Object>> result = records.getResult();
                for (Map<String, Object> map : result) {
                    Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, Object> entry = it.next();
                        if (entry.getKey() != null && entry.getKey().equals("TITLE")) {
                            Map<String, Object> t = new HashMap<String, Object>();
                            SystemCode sysCode = systemCodeService.load(entry.getValue().toString());
                            t.put("id", entry.getValue().toString());
                            if (sysCode != null) {
                                t.put("value", InternationalResource.get(sysCode.getValue()));
                            }
                            map.put("TITLE", t);
                        }
                    }
                }
            }
        }
        return records;
    }


    /**
     * 得到该角色的所有的specialpermissionshow配置信息
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/showRShowInfo")
    public Page<Map<String, Object>> showRShowInfo(Integer pageNo, Integer pageSize, Long roleId, Long operateId,
                                                   String specialPermissionCode, Boolean isAssigned, Boolean isTree,
                                                   String type) throws SQLException {
        Page<Map<String, Object>> records = new Page<Map<String, Object>>(pageNo, pageSize);
        // 根据queryConditionJson 生成SQL
        StringBuffer sqlSB = new StringBuffer();
        StringBuffer sqlPg = new StringBuffer();
        List<Object> parameters = new LinkedList<Object>();
        sqlSB.append("select  s.VALUE_ID ID,s.VALUE_TITLE as TITLE,s.VALUE_CODE as CODE,s.LAY_REC LAYREC,s.IS_INCLUDE_SUB ISINCLUDESUB  from EC_SPECIAL_PERMISSION_RSHOW s where s.valid=1  and  s.ROLE_Id=?  and  s.OPERATE_ID=?  and s.SPECIAL_PERMISSION_CODE=?  and s.IS_ASSIGNED=? ");
        sqlPg.append("select count(1) from EC_SPECIAL_PERMISSION_RSHOW show where show.VALID=1  and  show.ROLE_Id=?  and  show.OPERATE_ID=?  and show.SPECIAL_PERMISSION_CODE=?  and show.IS_ASSIGNED=? ");
        parameters.add(roleId);
        parameters.add(operateId);
        parameters.add(specialPermissionCode);
        parameters.add(isAssigned);
        Object[] params = new Object[parameters.size()];
        sqlSB.append(" order by s.ID ASC");
        records = specialPermissionForUShowService.findRecordPage(records, sqlSB.toString(), sqlPg.toString(), parameters.toArray(params));
        if (isTree != null && isTree && type != null && type.equals("SYSTEMCODE")) {
            List<Map<String, Object>> result = records.getResult();
            for (Map<String, Object> map : result) {
                Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = it.next();
                    if (entry.getKey() != null && entry.getKey().equals("TITLE")) {
                        Map<String, Object> t = new HashMap<String, Object>();
                        SystemCode sysCode = systemCodeService.load(entry.getValue().toString());
                        t.put("id", entry.getValue().toString());
                        if (sysCode != null) {
                            t.put("value", InternationalResource.get(sysCode.getValue()));
                        }
                        map.put("TITLE", t);
                    }
                }
            }
        }
        return records;
    }


    /**
     * 获得用户展现配置信息
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/loadUShowInfo")
    public List<String> loadUserPInfo() throws SQLException {
        List<String> permissionStr = new ArrayList<String>();
        try {
            HttpServletRequest request = getRequest();
            String opId = request.getParameter("operateId");
            String userId = request.getParameter("userId");
            String configedCode = request.getParameter("configCode");
            JSONArray specialPermissions = new JSONArray();
            if (opId != null && opId.trim().length() > 0 && userId != null && userId.trim().length() > 0) {
                List<String> configSpecialPermissionCodes = specialPermissionForUShowService.getConfigSpecialPermissonCode(Long.valueOf(userId), Long.valueOf(opId), configedCode);
                List<String> configSpecialPermissionNewCodes = new ArrayList<String>();
                for (String configCode : configSpecialPermissionCodes) {
                    if (configedCode.indexOf(configCode) == -1) {
                        configSpecialPermissionNewCodes.add(configCode);
                    }
                }
                int index = 0;
                for (String code : configSpecialPermissionNewCodes) {
                    StringBuilder idData = new StringBuilder();
                    StringBuilder layRecData = new StringBuilder();
                    StringBuilder isIncludeSubData = new StringBuilder();
                    SpecialPermission sp = specialPermissionForUShowService.loadSpecialPermission(code);
                    JSONObject o = new JSONObject();
                    o.put("pk", sp.getCode());
                    o.put("isTree", sp.getIsTree());
                    Property pkProperty = modelService.getPKProperty(sp.getTargetModelCode());
                    String pkColumnType = pkProperty.getType().toString();
                    String sptype = sp.getType();
                    o.put("bussinessPropertyColumnType", pkProperty.getType());
                    o.put("bussinessPropertyName", pkProperty.getCode());
                    if (sp.getType() != null && "OBJECT".equals(sp.getType())) {
                        o.put("associatePropertyCode", sp.getProperty().getAssociatedProperty().getCode());
                    }
                    o.put("type", sp.getType());
                    o.put("targetModelCode", sp.getTargetModelCode());
                    List<SpecialPermissionForUShow> uShowInfos = specialPermissionForUShowService.findAllShowInfo(Long.valueOf(userId), Long.valueOf(opId), code);
                    for (SpecialPermissionForUShow ushow : uShowInfos) {
                        if (sptype.length() > 0 && "OBJECT".equals(sptype)) {
                            if (pkColumnType.length() > 0 && "LONG".equals(pkColumnType)) {
                                idData.append(",").append(ushow.getValueId());
                            } else if (pkColumnType.length() > 0 && "TEXT".equals(pkColumnType)) {
                                idData.append(",|").append(ushow.getValueId()).append("|");
                            }
                        } else if (sptype.length() > 0 && "SYSTEMCODE".equals(sptype)) {
                            idData.append(",|").append(ushow.getValueId()).append("|");
                        }
                        if (ushow.getLayRec() != null && ushow.getLayRec().length() > 0) {
                            layRecData.append(",").append(ushow.getLayRec());
                        } else {
                            layRecData.append(",").append("");
                        }
                        if (ushow.getIsIncludeSub() != null && ushow.getIsIncludeSub().length() > 0) {
                            isIncludeSubData.append(",").append(ushow.getIsIncludeSub());
                        } else {
                            isIncludeSubData.append(",").append("false");
                        }
                    }
                    if (idData.length() > 0) {
                        idData.deleteCharAt(0);
                    }
                    if (layRecData.length() > 0) {
                        layRecData.deleteCharAt(0);
                    }
                    if (isIncludeSubData.length() > 0) {
                        isIncludeSubData.deleteCharAt(0);
                    }
                    o.put("content", idData);
                    o.put("layRecs", layRecData);
                    o.put("isIncludeSubDatas", isIncludeSubData);
                    specialPermissions.put(index, o);
                    permissionStr.add(o.toString());
                    index++;
                }

            }
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return permissionStr;
    }


    /**
     * 获得角色展现配置信息
     *
     * @return
     * @throws SQLException
     */
    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/loadRShowInfo")
    public List<String> loadRolePInfo() throws SQLException {
        List<String> permissionStr = new ArrayList<String>();
        try {
            HttpServletRequest request = getRequest();
            String opId = request.getParameter("operateId");
            String roleId = request.getParameter("roleId");
            String configedCode = request.getParameter("configCode");
            JSONArray specialPermissions = new JSONArray();
            if (opId != null && opId.trim().length() > 0 && roleId != null && roleId.trim().length() > 0) {
                List<String> configSpecialPermissionCodes = specialPermissionForRShowService.getConfigSpecialPermissonCode(Long.valueOf(roleId), Long.valueOf(opId));
                List<String> configSpecialPermissionNewCodes = new ArrayList<String>();
                for (String configCode : configSpecialPermissionCodes) {
                    if (configedCode.indexOf(configCode) == -1) {
                        configSpecialPermissionNewCodes.add(configCode);
                    }
                }
                int index = 0;
                for (String code : configSpecialPermissionNewCodes) {
                    StringBuilder idData = new StringBuilder();
                    StringBuilder layRecData = new StringBuilder();
                    StringBuilder isIncludeSubData = new StringBuilder();
                    SpecialPermission sp = specialPermissionForUShowService.loadSpecialPermission(code);
                    JSONObject o = new JSONObject();
                    o.put("pk", sp.getCode());
                    o.put("isTree", sp.getIsTree());
                    Property pkProperty = modelService.getPKProperty(sp.getTargetModelCode());
                    String pkColumnType = pkProperty.getType().toString();
                    String sptype = sp.getType();
                    o.put("bussinessPropertyColumnType", pkProperty.getType());
                    o.put("bussinessPropertyName", pkProperty.getCode());
                    if (sp.getType() != null && "OBJECT".equals(sp.getType())) {
                        o.put("associatePropertyCode", sp.getProperty().getAssociatedProperty().getCode());
                    }
                    o.put("type", sp.getType());
                    o.put("targetModelCode", sp.getTargetModelCode());
                    List<SpecialPermissionForRShow> uShowInfos = specialPermissionForRShowService.findAllShowInfo(Long.valueOf(roleId), Long.valueOf(opId), code);
                    for (SpecialPermissionForRShow ushow : uShowInfos) {
                        if (sptype.length() > 0 && "OBJECT".equals(sptype)) {
                            if (pkColumnType.length() > 0 && "LONG".equals(pkColumnType)) {
                                idData.append(",").append(ushow.getValueId());
                            } else if (pkColumnType.length() > 0 && "TEXT".equals(pkColumnType)) {
                                idData.append(",|").append(ushow.getValueId()).append("|");
                            }
                        } else if (sptype.length() > 0 && "SYSTEMCODE".equals(sptype)) {
                            idData.append(",|").append(ushow.getValueId()).append("|");
                        }
                        if (ushow.getLayRec() != null && ushow.getLayRec().length() > 0) {
                            layRecData.append(",").append(ushow.getLayRec());
                        } else {
                            layRecData.append(",").append("");
                        }
                        if (ushow.getIsIncludeSub() != null && ushow.getIsIncludeSub().length() > 0) {
                            isIncludeSubData.append(",").append(ushow.getIsIncludeSub());
                        } else {
                            isIncludeSubData.append(",").append("false");
                        }
                    }
                    if (idData.length() > 0) {
                        idData.deleteCharAt(0);
                    }
                    if (layRecData.length() > 0) {
                        layRecData.deleteCharAt(0);
                    }
                    if (isIncludeSubData.length() > 0) {
                        isIncludeSubData.deleteCharAt(0);
                    }
                    o.put("content", idData);
                    o.put("layRecs", layRecData);
                    o.put("isIncludeSubDatas", isIncludeSubData);
                    specialPermissions.put(index, o);
                    permissionStr.add(o.toString());
                    index++;
                }

            }
        } catch (NumberFormatException e) {
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
        return permissionStr;
    }


    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/loadUserpSpeicalpermissionConfig")
    public String loadUserPSpcialPermissionConfig() throws SQLException {
        String configString = null;
        HttpServletRequest request = getRequest();
        if (request.getParameter("id") != null && !request.getParameter("id").isEmpty()) {
            long id = Long.valueOf(request.getParameter("id"));
            UserPSpecialPermission ups = userPSpecialPermissionService.load(id);
            configString = ups.getConfigString();
            if (configString != null && configString.length() > 0) {
                configString = configString.replaceAll("\"", "'");
            }
        }
        return configString;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/specialPermission/loadRolepSpeicalpermissionConfig")
    public String loadRolePSpcialPermissionConfig() throws SQLException {
        String configString = null;
        HttpServletRequest request = getRequest();
        if (request.getParameter("id") != null && !request.getParameter("id").isEmpty()) {
            long id = Long.valueOf(request.getParameter("id"));
            RolePSpecialPermission rps = rolePSpecialPermissionService.load(id);
            configString = rps.getConfigString();
            //前台字符串转换需要
            if (configString != null && configString.length() > 0) {
                configString = configString.replaceAll("\"", "'");
            }
        }
        return configString;
    }

}