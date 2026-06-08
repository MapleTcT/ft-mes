package com.supcon.supfusion.configuration.services.openapi.controller;

import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.base.enums.SystemDisplayType;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.configuration.services.utils.JSONPlainSerializer;
import com.supcon.supfusion.configuration.services.utils.SqlParser;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Slf4j
@Controller
public class SystemCodeController extends ConfigurationBaseController {

    @Autowired
    private SystemCodeService systemCodeService;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private InternationalService internationalService;

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/systemCodeJson")
    public Map systemCodeJson(String systemEntityCode) {
        Map<String, String> systemCodeMap = new LinkedHashMap<String, String>();
        if (systemEntityCode != null && !systemEntityCode.equals("")) {
            systemCodeMap = systemCodeService.getSystemCodeList(systemEntityCode, false);
        }
        return systemCodeMap;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/getSystemEntityForProperty")
    public Map getSystemEntityForProperty(String entityCode) {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        SystemEntity systemEntity = systemCodeService.getSystemEntityByCode(entityCode);
        if (systemEntity != null) {
            responseMap.put("companyType", "UNIT");
            responseMap.put("listType", systemEntity.getListType().toString());
        }
        return responseMap;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/getModuleCode")
    public String getModuleCodeWithSystemEntityCode(String entityCode) {
        SystemEntity sysEntity = systemCodeService.getSystemEntityByCode(entityCode);
        if (sysEntity != null) {
            return sysEntity.getModuleCode();
        }
        return StringUtils.EMPTY;
    }

    @RequestMapping(value = "/ec/systemCode/codeValueManager/defaultForProperty")
    public String valueManagerForProperty(ModelMap map, String systemEntityCode, String requstObjectType) {
        map.put("systemEntityCode", systemEntityCode);
        map.put("requstObjectType", requstObjectType);
        return "systemCode/valueManagerForProperty";
    }

    @RequestMapping(value = {"/ec/systemCode/addCode/default", "/ec/systemCode/modifyCode/default"})
    public String systemEntityEdit(ModelMap map, Long entityId) {
        Map<String, String> listType = new LinkedHashMap<String, String>();
        listType.put(SystemDisplayType.list.toString(), InternationalResource.get("foundation.systemCode.listType.list"));
        listType.put(SystemDisplayType.tree.toString(), InternationalResource.get("foundation.systemCode.listType.tree"));
        String namespace;
        SystemEntity systemEntity = null;
        if (entityId != null) {
            systemEntity = systemCodeService.getSystemEntity(entityId);
            namespace = "/ec/systemCode/modifyCode";
        } else {
            systemEntity = new SystemEntity();
            systemEntity.setModuleCode(getRequest().getParameter("systemEntity.moduleCode"));
            namespace = "/ec/systemCode/addCode";
        }
        Module module = moduleService.getModule(getRequest().getParameter("systemEntity.moduleCode"));
        map.put("listType", listType);
        map.put("systemEntity", systemEntity);
        map.put("namespace", namespace);
        map.put("moduleName", internationalService.getI18nValue(module.getName()));

        return "systemCode/codeEdit";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/codeValueManager/valueList")
    public Page<SystemCode> valueList(String systemEntityCode, @Nullable String systemCodeCode, @Nullable String systemValue, Integer pageNo, Integer pageSize) {
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SystemCode.class);
        detachedCriteria.add(Restrictions.eq("valid", true));

        if (systemEntityCode != null && !systemEntityCode.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(" {alias}.entity_code = ?", systemEntityCode, new StringType()));
        }
        if (systemCodeCode != null && !systemCodeCode.equals("")) {
            detachedCriteria.add(Restrictions.sqlRestriction(" {alias}.code like ? escape '&'", "%" + SqlParser.filtrateSQLLike(systemCodeCode) + "%", new StringType()));
        }
        if (systemValue != null && !systemValue.equals("")) {

            List<Object> keyLists = InternationalResource.getMessageKeys(SqlParser.filtrateSQLLike(systemValue));
            StringBuffer sqlRestriction = new StringBuffer(" {alias}.value in (");
            if (null != keyLists) {
                for (int i = 0; i < keyLists.size(); i++) {
                    sqlRestriction.append(" ?,");
                }
                sqlRestriction.deleteCharAt(sqlRestriction.length() - 1);
            }
            sqlRestriction.append(")");
            if (null != keyLists)
                detachedCriteria.add(Restrictions.sqlRestriction(sqlRestriction.toString(), keyLists.toArray(), DbUtils.getHibernateTypeByJavaType(keyLists)));
        }
        detachedCriteria.add(Restrictions.eq("company", getCurrentCompany()));
        Page<SystemCode> page = new Page<>(pageNo, pageSize);
        Page<SystemCode> systemCodePage = systemCodeService.getBySystemCodePage(page, detachedCriteria);

        return systemCodePage;
    }

    @RequestMapping(value = "/ec/systemCode/codeValueManager/valueTreeManagerForProperty")
    public String valueTreeManagerForPropert(ModelMap map, String systemEntityCode, String requstObjectType) {
        SystemEntity systemEntity = systemCodeService.getSystemEntityByCode(systemEntityCode);
        map.put("systemEntityCode", systemEntityCode);
        map.put("requstObjectType", requstObjectType);
        map.put("systemEntity", systemEntity);
        return "systemCode/valueTreeManagerForProperty";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/codeValueSort/save")
    public Map codeValueSortSave(String systemCodeCode) {
        String[] arr = systemCodeCode.split(";");
        for (String str : arr) {
            String[] info = str.split(",");
            SystemCode sc = systemCodeService.load(info[0]);
            sc.setSort(Long.parseLong(info[1]));
            systemCodeService.saveSystemCode(sc);
        }
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put("dealSuccessFlag", true);
        return responseMap;
    }

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/codeValueManager/valueTreeList")
    public String valueTreeList(String systemEntityCode, String id) {
        String exclude = "*.class,*.createStaffId,*.modifyStaffId,*.deleteStaffId,*.createTime,*.modifyTime," + "*.deleteTime,*.manager,*.modifyStaff,*.createStaff,*.deleteStaff,*.Cid," + "*.company,*.sort,*.version,*.uniqueCode,*.root,*.attribute,*.type,*.valid," + "*.defaultFlag";
        String[] e = exclude.split(",");
        String include = "*.code,*.value,*.id,*.isParent,*.version,*.entityCode,*.layRec,*.children";
        JSONPlainSerializer serializer = new JSONPlainSerializer();
        serializer.exclude(e);
        serializer.include(include.split(","));

        SystemCode systemCode = null;
        Set<SystemCode> tree = null;
        if (systemEntityCode != null && !systemEntityCode.equals("")) {
            if (id != null) {
                systemCode = new SystemCode();
                systemCode.setId(id);
            }
            tree = systemCodeService.getTreeList(getCurrentCompany(), systemEntityCode, systemCode);
        }
        localiseSystemCodeTree(tree);
        return serializer.deepSerialize(tree);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/systemCode/codeValueManager/SystemCodeinfo")
    public SystemCode systemCodeinfo(@RequestParam("systemCode.id") String id) {
        SystemCode systemCode = null;
        if (!id.equals("-1")) {
            systemCode = systemCodeService.getSystemCode(id);
            systemCode.setCompany(getCurrentCompany());
            if (systemCode.getParentId() != null)
                systemCode.setParent(systemCodeService.getSystemCode(systemCode.getParentId()));
        }
        return systemCode;
    }

    @RequestMapping(value = "/ec/foundation/systemCode/addValue/treeValueadd")
    public String treeValueAdd(ModelMap map, String systemEntityCode, @Nullable @RequestParam("systemCode.id") String systemCodeId) {
        SystemEntity systemEntity = systemCodeService.getSystemEntityByCode(systemEntityCode);
        SystemCode parentsystemCode = null;
        if (systemCodeId != null)
            parentsystemCode = (SystemCode) systemCodeService.getSystemCode(systemCodeId);
        map.put("systemEntity", systemEntity);
        map.put("parentsystemCode", parentsystemCode);
        return "systemCode/treeValueadd";
    }

    @RequestMapping(value = "/ec/foundation/systemCode/addValue/treeValuemodify")
    public String treeValuemodify(ModelMap map, @Nullable @RequestParam("systemCode.id") String systemCodeId) {
        SystemCode systemCode = systemCodeService.getSystemCode(systemCodeId);
        if (systemCode.getParentId() != null) {
            SystemCode parentsystemCode = systemCodeService.getSystemCode(systemCode.getParentId());
            systemCode.setParent(parentsystemCode);
        }
        map.put("systemCode", systemCode);
        return "systemCode/treeValuemodify";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/foundation/systemCode/deleteValue/default")
    public Map sysValueDelete(@Nullable @RequestParam("systemCode.id") String systemCodeId, @Nullable @RequestParam("systemCode.version") Integer systemCodeVersion) throws Exception {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        if (systemCodeId != null) {
            systemCodeService.deleteAndChildren(systemCodeId, systemCodeVersion);
            responseMap.put("dealSuccessFlag", true);
        }
        return responseMap;
    }

    @ResponseBody
    @RequestMapping(value = {"/ec/foundation/systemCode/addValue/systemCodeSave", "/ec/foundation/systemCode/modifyValue/systemCodeSave"})
    public Map systemCodeSave(String systemEntityCode, String strType, @RequestParam("systemCode.code") String systemCodeCode, @RequestParam("systemCode.value") String systemCodeValue, @RequestParam("systemCode.cid") Long cid, @RequestParam("systemCode.memo") String memo, @RequestParam("systemCode.zhCnValue") String zhCnValue) {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        SystemCode systemCode = new SystemCode();
        systemCode.setCode(systemCodeCode);
        systemCode.setValue(systemCodeValue);
        systemCode.setCid(cid);
        systemCode.setMemo(memo);
        systemCode.setValueZhCn(zhCnValue);
        if (systemCode != null && systemEntityCode != null && !systemEntityCode.equals("")) {

            systemCode.setValid(true);
            systemCode.setEntityCode(systemEntityCode);
            systemCodeService.saveSystemCode(systemCode, strType);
            String[] s = systemCode.getId().split("/");
            String systemCodeId = systemCode.getEntityCode() + "/" + s[1];
            // 默认值修改
            if (systemCode.getDefaultFlag() != null && systemCode.getDefaultFlag()) {
                SystemEntity systemEntity = systemCodeService.getSystemEntityByCode(systemEntityCode);
                Map<String, SystemCode> map = systemEntity.getSystemCodes();
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    SystemCode sc = (SystemCode) map.get(key);
                    if (sc.getId().equals(systemCodeId)) {
                        sc.setDefaultFlag(true);
                    } else {
                        sc.setDefaultFlag(false);
                    }
                    systemCodeService.saveSystemCode(sc);
                }
            }
            responseMap.put("dealSuccessFlag", true);
            responseMap.put("id", systemCode.getId());
            responseMap.put("systemEntityCode", systemEntityCode);
            responseMap.put("value", InternationalResource.get(systemCode.getValue()));
        }
        // 修改保存
        else {
            if (systemCode != null) {
                SystemCode old = (SystemCode) systemCodeService.getSystemCode(systemCode.getId());
                old.setValue(systemCode.getValue());
                old.setMemo(systemCode.getMemo());
                systemCodeService.saveSystemCode(old, strType);
                //                systemCodeId = old.getId();
                responseMap.put("dealSuccessFlag", true);
                responseMap.put("id", systemCode.getId());
            }
        }
        return responseMap;
    }

    @ResponseBody
    @RequestMapping(value = {"ec/systemCode/modifyCode/systemEntitySave", "ec/systemCode/addCode/systemEntitySave"})
    public Map systemEntitySave(HttpServletRequest request) {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        SystemEntity systemEntity = DtoUtils.getSystemEntity(request);
        systemCodeService.saveSystemEntity(systemEntity);
        responseMap.put("dealSuccessFlag", true);
        responseMap.put("id", systemEntity.getId());
        return responseMap;
    }

    private void localiseSystemCodeTree(Collection<SystemCode> tree) {
        if (tree == null || tree.isEmpty()) {
            return;
        }
        for (SystemCode sc : tree) {
            sc.setValue(InternationalResource.get(sc.getValue()));
            localiseSystemCodeTree(sc.getChildren());
        }
    }

}
