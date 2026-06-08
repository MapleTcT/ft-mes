package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.dao.SystemCodeDaoImpl;
import com.supcon.supfusion.base.dao.SystemCodePODaoImpl;
import com.supcon.supfusion.base.dao.SystemEntityDaoImpl;
import com.supcon.supfusion.base.dao.SystemEntityPODaoImpl;
import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.entities.SystemCodePO;
import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.base.entities.SystemEntityPO;
import com.supcon.supfusion.base.enums.CompanyType;
import com.supcon.supfusion.base.enums.SystemDisplayType;
import com.supcon.supfusion.base.services.CompanyService;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.service.PropertyKeyService;
import com.supcon.supfusion.configuration.services.service.SystemCodeService;
import com.supcon.supfusion.configuration.services.utils.FileUtils;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.systemcode.api.SystemCodeApiService;
import com.supcon.supfusion.systemcode.api.SystemEntityApiService;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityAddDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Comparator.comparing;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class SystemCodeServiceImpl implements SystemCodeService {

    @Autowired
    private SystemCodeDaoImpl systemCodeDao;
    @Autowired
    private SystemEntityDaoImpl systemEntityDao;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private SystemCodeApiService systemCodeApiService;
    @Autowired
    private SystemEntityApiService systemEntityApiService;
    @Autowired
    private InternationalService internationalService;
    @Autowired
    private PropertyKeyService propertyKeyService;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private SystemCodePODaoImpl systemCodePODao;
    @Autowired
    private SystemEntityPODaoImpl systemEntityPODao;
    private static Pattern p = Pattern.compile("^(?!_)(?!.*?_$)[a-zA-Z0-9_]+$");
    private static final String PATTERN_CODE = "^[A-Za-z0-9_]{0,100}$";

    @Override
    public SystemCode load(String code) {
        return systemCodeDao.get(code);
    }

    @Override
    public void deleteSystemEntityAndCode(String moduleCode) {
        if (StringUtils.isBlank(moduleCode)) {
            return;
        }
        // 逻辑删除系统编码
        systemCodePODao.createNativeQuery("UPDATE " + SystemCodePO.TABLE_NAME + " SET valid = 0 WHERE entity_code in(SELECT code FROM sys_entity WHERE module_id = ?)", moduleCode).executeUpdate();
        // 删除编码值
        systemEntityPODao.createNativeQuery("UPDATE " + SystemEntityPO.TABLE_NAME + " SET valid = 0 WHERE module_id = ?", moduleCode).executeUpdate();
    }

    @Override
    public void saveSystemCode(SystemCode systemCode) {
        if (!Pattern.matches(PATTERN_CODE, systemCode.getCode())) {
            throw new EcException(EcException.Code.CONTRAINT_CODE);
        }
        SystemCodePO existSystemCode = systemCodePODao.findEntityByCriteria(Restrictions.eq("valid", 1), Restrictions.eq("entityCode", systemCode.getEntityCode()), Restrictions.eq("code", systemCode.getCode()));
        if (null != existSystemCode) {
            Long rowVersion = existSystemCode.getRowVersion() + 1;
            BeanUtil.copy(systemCode, existSystemCode);
            existSystemCode.setDesA(systemCode.getCodeDesA());
            existSystemCode.setDesB(systemCode.getCodeDesB());
            existSystemCode.setDesC(systemCode.getCodeDesC());
            existSystemCode.setRowVersion(rowVersion);
            if (null != systemCode.getSort()) {
                existSystemCode.setSort(systemCode.getSort().doubleValue());
            }
            if (StringUtils.isNotEmpty(systemCode.getParentId()) && StringUtils.isNumeric(systemCode.getParentId())) {
                existSystemCode.setParentId(Long.valueOf(systemCode.getParentId()));
            } else {
                existSystemCode.setParentId(null);
            }
            existSystemCode.setLayRec(systemCode.getLayRec());
            existSystemCode.setFullPathName(systemCode.getFullPathName());
            existSystemCode.setFullPath(systemCode.getFullPath());
            existSystemCode.setDefaultFlag(systemCode.getDefaultFlag() ? 1 : 0);
            updateValue(existSystemCode);
        } else {
            SystemCodePO newSystemCode = BeanUtil.copy(systemCode, SystemCodePO.class);
            if (systemCode.getPoId() != null) {
                newSystemCode.setId(systemCode.getPoId());
            }
            newSystemCode.setDesA(systemCode.getCodeDesA());
            newSystemCode.setDesB(systemCode.getCodeDesB());
            newSystemCode.setDesC(systemCode.getCodeDesC());
            newSystemCode.setRowVersion(0L);
            newSystemCode.setDefaultFlag(systemCode.getDefaultFlag() ? 1 : 0);
            if (StringUtils.isNotEmpty(systemCode.getParentId()) && StringUtils.isNumeric(systemCode.getParentId())) {
                newSystemCode.setParentId(Long.valueOf(systemCode.getParentId()));
            }
            newSystemCode.setLayRec(systemCode.getLayRec());
            newSystemCode.setFullPathName(systemCode.getFullPathName());
            newSystemCode.setFullPath(systemCode.getFullPath());
            if (null != systemCode.getSort()) {
                newSystemCode.setSort(systemCode.getSort().doubleValue());
            }
            insertValue(newSystemCode);
        }
    }

    @Override
    public void saveSystemCodeAndXml(SystemCode systemCode) {
        saveSystemCode(systemCode);
        systemCodePODao.flush();
        SystemEntity systemEntity = this.getSystemEntityByCode(systemCode.getEntityCode());
        Module module = moduleService.getModule(systemEntity.getModuleCode());
        SystemCodePO systemCodePO = systemCodePODao.findEntityByCriteria(Restrictions.eq("valid", 1), Restrictions.eq("entityCode", systemCode.getEntityCode()), Restrictions.eq("code", systemCode.getCode()));
        SystemCode sc = BeanUtil.copyProperties(systemCodePO, SystemCode.class);
        sc.setCodeDesA(systemCodePO.getDesA());
        sc.setCodeDesB(systemCodePO.getDesB());
        sc.setCodeDesC(systemCodePO.getDesC());
        sc.setSort(systemCode.getSort());
        FileUtils.updateSystemCodeXml(module, sc);
    }

    public void updateValue(SystemCodePO systemCodePO) {
        // 如果修改编码值的默认属性设置为是,则其他的值都为否
        if (1 == systemCodePO.getDefaultFlag()) {
            systemCodePODao.createNativeQuery("UPDATE " + SystemCodePO.TABLE_NAME + " SET default_flag = 0, row_version = row_version + 1 WHERE valid = 1 AND default_flag = 1 AND entity_code = ?  AND code != ?", systemCodePO.getEntityCode(), systemCodePO.getCode()).executeUpdate();
        }
        systemCodePODao.save(systemCodePO);
    }

    public String queryDisplayName(SystemCodePO systemCodePO) {
        String displayName = internationalService.getI18nValue(systemCodePO.getName());
        if (StringUtils.isBlank(displayName)) {
            displayName = systemCodePO.getDisplayName();
        }
        return displayName;
    }

    private void insertValue(SystemCodePO systemCodePO) {
        // 如果修改编码值的默认属性设置为是,则其他的值都为否
        if (1 == systemCodePO.getDefaultFlag()) {
            systemCodePODao.createNativeQuery("UPDATE " + SystemCodePO.TABLE_NAME + " SET default_flag = 0, row_version = row_version + 1 WHERE valid = 1 AND default_flag = 1 AND entity_code = ?  AND code != ?", systemCodePO.getEntityCode(), systemCodePO.getCode()).executeUpdate();
        }
        if (systemCodePO.getId() == null) {
            Long id = IDGenerator.newInstance().generate().longValue();
            systemCodePO.setId(id);
        }
        if (systemCodePO.getParentId() == null && systemCodePO.getParentCode() == null) {
            systemCodePO.setLayNo(1);
            systemCodePO.setFullPath(systemCodePO.getCode());
            systemCodePO.setFullPathName(queryDisplayName(systemCodePO));
            systemCodePO.setLayRec(String.valueOf(systemCodePO.getId()));
            if (systemCodePO.getSort() == null) {
                Number maxSort = (Number) systemCodePODao.createNativeQuery("SELECT MAX(sort) FROM " + SystemCodePO.TABLE_NAME + " WHERE valid = 1 AND parent_id IS NULL").uniqueResult();
                systemCodePO.setSort(maxSort.doubleValue() + 1.0);
            }
        } else {
            SystemCodePO parentSystemCodePo = null;
            if (systemCodePO.getParentId() != null) {
                parentSystemCodePo = systemCodePODao.load(systemCodePO.getParentId());
            } else if (systemCodePO.getParentCode() != null) {
                List<SystemCodePO> list = systemCodePODao.findByCriteria(Restrictions.eq("entityCode", systemCodePO.getEntityCode()),
                        Restrictions.eq("code", systemCodePO.getParentCode()));
                if (list != null && !list.isEmpty()) {
                    parentSystemCodePo = list.get(0);
                }
            }
            if (parentSystemCodePo != null) {
                systemCodePO.setLayNo(parentSystemCodePo.getLayNo() + 1);
                systemCodePO.setFullPath(parentSystemCodePo.getFullPath() + "/" + systemCodePO.getCode());
                systemCodePO.setFullPathName(parentSystemCodePo.getFullPathName() + "/" + queryDisplayName(systemCodePO));
                systemCodePO.setLayRec(parentSystemCodePo.getLayRec() + "-" + systemCodePO.getId());
                systemCodePO.setLeaf(true);
                systemCodePO.setParentId(parentSystemCodePo.getId());
                if (systemCodePO.getSort() == null) {
                    Number maxSort = (Number) systemCodePODao.createNativeQuery("SELECT MAX(sort) FROM " + SystemCodePO.TABLE_NAME + " WHERE valid = 1 AND parent_id = ?", parentSystemCodePo.getId()).uniqueResult();
                    if (maxSort != null) {
                        systemCodePO.setSort(maxSort.doubleValue() + 1.0);
                    } else {
                        systemCodePO.setSort(1.0);
                    }
                }
            }
        }
        systemCodePO.setValid(1);
        systemCodePODao.save(systemCodePO);
        systemCodePODao.flush();
    }

    @Override
    public void saveSystemCode(SystemCode systemCode, String strType) {
        boolean b = p.matcher(systemCode.getCode()).matches();
        if (!b) {
            throw new EcException(EcException.Code.CONTRAINT_CODE);
        }
        if (null != systemCode.getValueZhCn()) {
            if (systemCode.getValueZhCn().indexOf("<") > 0 || systemCode.getValueZhCn().indexOf(">") > 0) {
                throw new EcException(EcException.Code.SYSTEMCODE_SPECIAL_CHAR_ERROR);
            }
        }
        if ("add".equals(strType)) {
            // 检查编码是否唯一
            if (!checkSysCodeCodeUnique(systemCode.getCode(), systemCode.getEntityCode())) {
                throw new EcException(EcException.Code.UNIQUECODE);
            }
        }
        saveSystemCodeAndXml(systemCode);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean checkSysCodeCodeUnique(String sysCodeCode, String entityCode) {
        String hql = "";
        hql = "select count(s.id) as totalCoual from SystemCode as s where upper(s.code)=? and s.entityCode=?";
        List<Object> parameters = new LinkedList<Object>();
        parameters.add(sysCodeCode.toUpperCase());
        parameters.add(entityCode);
        Object[] params = new Object[parameters.size()];
        List<Long> codeCodeCount = systemCodeDao.findByHql(hql, parameters.toArray(params));
        if (codeCodeCount.get(0) == 0) {
            return true;
        }
        return false;
    }

    @Override
    public void saveSystemEntity(SystemEntity systemEntity) {
        checkSystemEntity(systemEntity);
        String internationalName = systemEntity.getName();
        if (internationalName.indexOf("$&#") > 0) {
            systemEntity.setName(internationalName.split("\\$&#")[0].substring(4));
        }
        if (!Pattern.matches(PATTERN_CODE, systemEntity.getCode())) {
            throw new EcException(EcException.Code.CONTRAINT_CODE);
        }
        SystemEntityPO existSystemEntity = null;
        if (systemEntity.getId() == null) {
            existSystemEntity = systemEntityPODao.findEntityByCriteria(Restrictions.eq("valid", 1), Restrictions.eq("code", systemEntity.getCode()));
        }
        if (existSystemEntity != null) {
            log.info("systemEntity已存在，code:{}", systemEntity.getCode());
            throw new EcException("编码已存在");
        }
        SystemEntityPO systemEntityPO = BeanUtil.copy(systemEntity, SystemEntityPO.class);
        log.info("开始保存系统编码，code=" + systemEntityPO.getCode());

        systemEntityPO.setValid(1);
        if (systemEntityPO.getMultiFlag() == null) {
            systemEntityPO.setMultiFlag(0);
        }
        if (systemEntityPO.getSysDefault() == null) {
            systemEntityPO.setSysDefault(0);
        }
        if (systemEntity.getId() == null) {
            Long id = IDGenerator.newInstance().generate().longValue();
            systemEntityPO.setId(id);
//            if (!systemEntityPO.getCode().startsWith(systemEntityPO.getModuleId())) {
//                systemEntityPO.setCode(systemEntityPO.getModuleId() + "_" + systemEntityPO.getCode());
//            }
            systemEntityPODao.save(systemEntityPO);
            systemEntity.setId(id);
        } else {
            systemEntityPODao.update(systemEntityPO);
        }
        internationalService.addInternational(internationalName);
    }

    private void checkSystemEntity(SystemEntity systemEntity) {
        if (StringUtils.isEmpty(systemEntity.getCode())) {
            throw new EcException("编码不能为空");
        }
        if (StringUtils.isEmpty(systemEntity.getName())) {
            throw new EcException("名称不能为空");
        }
        String code = systemEntity.getCode();// java关键字
        if (!propertyKeyService.checkJavaKey(code) || !propertyKeyService.checkBapKey(code)) {
            log.error("不允许使用关键字：" + code);
            throw new EcException(EcException.Code.KEY);
        }
    }

    private boolean checkSysCodeUnique(String sysClassCode) {
        String hql = "";
        hql = "select count(s.id) as totalCoual from SystemEntity as s where s.code=? and s.valid=true";
        List<Long> ClassCodeCount = systemEntityDao.findByHql(hql, sysClassCode);
        if (ClassCodeCount.get(0) == 0) {
            return true;
        }
        return false;
    }

    @Override
    public SystemEntity getSystemEntityByCode(String entityCode) {
        SystemEntity systemEntity = systemEntityDao.findEntityByCriteria(Restrictions.eq("code", entityCode), Restrictions.eq("valid", true));
        if (null != systemEntity) {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SystemCode.class);
            detachedCriteria.add(Restrictions.eq("valid", true));
            detachedCriteria.add(Restrictions.eq("entityCode", entityCode));
            detachedCriteria.addOrder(Order.asc("sort"));
            detachedCriteria.addOrder(Order.asc("id"));
            List<SystemCode> systemCodes = systemCodeDao.findByCriteria(detachedCriteria);
            for (SystemCode systemCode : systemCodes) {
                systemEntity.putSystemCode(systemCode.getCode(), systemCode);
            }
        }
        return systemEntity;
    }

    @Override
    public List<SystemCode> getSystemCodeByEntity(String entityCode) {
        if ("foundation".equals(entityCode)) {
            entityCode = "sys";
        }
        return systemCodeDao.findByHql("from SystemCode where entityCode=? and valid=true", entityCode);
    }

    @Override
    public List<SystemEntity> getSystemEntityLists(Company company) {
        List<SystemEntity> list = null;
        if (CompanyType.GROUP.equals(company.getType())) {
            list = systemEntityDao.createCriteria(Restrictions.or(Restrictions.eq("cid", 0L), Restrictions.eq("cid", company.getId())), Restrictions.eq("valid", true)).addOrder(Order.desc("id")).list();

        } else if (CompanyType.ORGANIZATION.equals(company.getType())) {
            list = systemEntityDao.createCriteria(Restrictions.or(Restrictions.eq("cid", 0L), Restrictions.eq("cid", company.getId())), Restrictions.eq("valid", true)).addOrder(Order.desc("id")).list();

        } else {
            list = systemEntityDao.createCriteria(Restrictions.eq("valid", true)).addOrder(Order.desc("id")).list();
        }
        return list;
    }

    @Override
    public SystemCode getSystemCode(String systemCodeID) {
        SystemCode systemCode = systemCodeDao.get(systemCodeID);
        if (systemCode != null && systemCode.getValue() != null) {
            systemCode.setValue(systemCode.getValueZhCn());
        }
        return systemCode;
    }

    @Override
    public Map<String, String> getSystemCodeMap(String systemEntityCode) {
        SystemEntity systemEntity = null;
        Map<String, String> result = new LinkedHashMap<String, String>();
        systemEntity = getSystemEntityByCode(systemEntityCode);

        if (null != systemEntity) {
            for (SystemCode ss : systemEntity.getSystemCodes().values()) {
                SystemCode sc = ss;
                Long cid = sc.getCid();
                String interValue = sc.getValue();
                if (cid.equals(0L)) {
                    result.put(sc.getCode(), interValue);
                }
                try {
                    sc.setCompany(companyService.get(cid));
                } catch (Exception e) {
                }
                result.put(sc.getCode(), interValue);
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getSystemCodeList(String systemEntityCode, Boolean senior) {
        SystemEntity systemEntity = null;
        Map<String, String> result = new LinkedHashMap<String, String>();
        systemEntity = getSystemEntityByCode(systemEntityCode);

        if (null != systemEntity) {
            for (SystemCode ss : systemEntity.getSystemCodes().values()) {
                SystemCode sc = (SystemCode) ss;
                Long cid = sc.getCid();
                String interValue = sc.getValue();
                interValue=internationalService.getI18nValue(interValue);
                if (cid.equals(0L)) {
                    result.put((senior ? sc.getCode() : sc.getId()), interValue);
                }
                try {
                    sc.setCompany(companyService.get(cid));
                } catch (Exception e) {
                }
                result.put((senior ? sc.getCode() : sc.getId()), interValue);
            }
        }
        return result;
    }

    @Override
    public void initializeSystemCode(URL url) throws XMLStreamException, IOException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        InputStream io = url.openStream();
        XMLStreamReader r = factory.createXMLStreamReader(io);
        try {
            this.initializeSystemCode(r);
        } finally {
            io.close();
        }
    }

    @Override
    public SystemEntity getSystemEntity(Long entityId) {
        SystemEntity systemEntity = systemEntityDao.load(entityId);
        return systemEntity;
    }

    private synchronized void initializeSystemCode(XMLStreamReader r) throws XMLStreamException {
        Stack<SystemEntity> entityStack = new Stack<SystemEntity>();
        Stack<SystemCode> codeStack = new Stack<SystemCode>();

        List<SystemCode> systemCodeList = new ArrayList<>();
        // key: originSystemCodeId value: systemCode
        Map<Long, SystemCode> originSystemCodeMap = new HashMap<>();
        // key: entityCode.code + / + systemcode.code, value: systemCode
        Map<String, SystemCode> originStrSystemCodeMap = new HashMap<>();
        Long cid = 1l;
        while (r.hasNext()) {
            r.next();
            if (r.isStartElement()) {
                Map<String, Object> attributeMap = new HashMap<String, Object>();
                for (int i = 0; i < r.getAttributeCount(); ++i) {
                    attributeMap.put(r.getAttributeName(i).getLocalPart(), r.getAttributeValue(i));
                }
                attributeMap.put("cid", String.valueOf(Company.defaultCompanyId));
                if (r.getName().toString().equals("systementity")) { // 注意 要从stack中拿到SYS_CLASS_CODE

                    SystemEntity sysEntity = systemEntityDao.findEntityByCriteria(Restrictions.eq("code", String.valueOf(attributeMap.get("code"))), Restrictions.eq("valid", true));
                    if (null != sysEntity && !sysEntity.isValid()) {
                        continue;
                    }
                    if (null == attributeMap.get("listType") || "".equals(attributeMap.get("listType"))) {
                        continue;
                    }
                    // 系统编码若已存在，且所属模块不一样，直接报错，提示用户系统编码已存在。
                    String moduleCode1 = String.valueOf(attributeMap.get("moduleCode"));
                    if (moduleCode1.indexOf("_") > 0) {
                        moduleCode1 = moduleCode1.split("_")[0];
                    }
                    if (sysEntity != null && !moduleCode1.equals(sysEntity.getModuleCode()) && !"sysbase_1.0".equals(String.valueOf(attributeMap.get("moduleCode")))) {
                        throw new RuntimeException("foundation.systemcode.systomcode" + attributeMap.get("code") + "foundation.systemcode.hassystomcode");
                    }
                    if (null != sysEntity && Long.valueOf((String) attributeMap.get("version")) < sysEntity.getVersion()) {
                        log.info("SystemEntity(code=" + (String) attributeMap.get("code") + ")对象xml文件中的version小于或等于数据库中的version，不更新数据库记录");
                    } else {
                        if (null == sysEntity) {
                            sysEntity = new SystemEntity();
                        }
                        String code = String.valueOf(attributeMap.get("code"));
                        String moduleCode = String.valueOf(attributeMap.get("moduleCode"));
                        String name = String.valueOf(attributeMap.get("name"));
                        Boolean sysDefault = Boolean.valueOf(String.valueOf(attributeMap.get("sysDefault")));
                        SystemDisplayType listType = SystemDisplayType.valueOf(String.valueOf(attributeMap.get("listType")).toLowerCase());
                        Boolean multiFlag = Boolean.valueOf(String.valueOf(attributeMap.get("multiFlag")));
                        String memo = String.valueOf(attributeMap.get("memo"));
                        String cidStr = (String) attributeMap.get("cid");

                        if (cidStr != null && cidStr.length() > 0) {
                            cid = Long.valueOf(cidStr);
                        }

                        Boolean valid = Boolean.valueOf(String.valueOf(attributeMap.get("valid")));
                        Integer version = Integer.valueOf(String.valueOf(attributeMap.get("version")));
                        sysEntity.setCompany(companyService.get(cid));
                        if (null != code && code.trim().length() > 0)
                            sysEntity.setCode(code);
                        if (null != moduleCode && moduleCode.trim().length() > 0)
                            sysEntity.setModuleCode(moduleCode);
                        if (null != name && name.trim().length() > 0)
                            sysEntity.setName(name);
                        if (null != sysDefault)
                            sysEntity.setSysDefault(sysDefault);
                        if (null != listType)
                            sysEntity.setType(listType.toString().toLowerCase());
                        if (null != multiFlag)
                            sysEntity.setMultiFlag(multiFlag);
                        if (null != memo)
                            sysEntity.setMemo(memo);
                        if (null != cid)
                            sysEntity.setCid(cid);
                        if (null != valid)
                            sysEntity.setValid(valid);
                        if (null != version)
                            sysEntity.setVersion(version);
                        saveSystemEntity(sysEntity);
                    }
                    entityStack.add(sysEntity);
                } else if (r.getName().toString().equals("systemcode")) { // 注意 要从stack中拿到ENTITY_CODE, 并计算出LAY_NO和LAY_REC
                    if (null == attributeMap.get("id") || "".equals(attributeMap.get("id"))) {
                        continue;
                    }
                    SystemCode syscode = systemCodeDao.findEntityByCriteria(Restrictions.eq("id", String.valueOf(attributeMap.get("id"))), Restrictions.eq("valid", true));
                    if (null != syscode && !syscode.isValid()) {
                        continue;
                    }
                    if (null != syscode && Long.valueOf((String) attributeMap.get("version")) < syscode.getVersion()) {
                        log.info("SystemCode(code=" + (String) attributeMap.get("code") + ")对象xml文件中的version小于或等于数据库中的version，不更新数据库记录");
                    } else {
                        String id = String.valueOf(attributeMap.get("id"));
                        if (null == syscode) {
                            syscode = new SystemCode();// id="119" code="STAFFSTATUTS_03" value="离职" attribute="false"														// memo="" sort="" valid="true" version="0" cid="1"
                            syscode.setPoId(IDGenerator.newInstance().generate().longValue());
                        } else {
                            String[] idSplit = id.split("/");
                            String systemEntityCode = idSplit[0];
                            String systemCodeCode = idSplit[1];
                            SystemCodePO systemCodePO = systemCodePODao.findEntityByCriteria(Restrictions.eq("valid", 1), Restrictions.eq("entityCode", systemEntityCode), Restrictions.eq("code", systemCodeCode));
                            syscode.setPoId(systemCodePO.getId());
                        }
                        String code = String.valueOf(attributeMap.get("code"));
                        String value = String.valueOf(attributeMap.get("value"));
                        Boolean attribute = Boolean.valueOf(String.valueOf(attributeMap.get("attribute")));
                        String memo = String.valueOf(attributeMap.get("memo"));
                        Boolean leaf = Boolean.valueOf(String.valueOf(attributeMap.get("leaf")));
                        Boolean defaultFlag = Boolean.valueOf(String.valueOf(attributeMap.get("defaultFlag")));
                        String codeDesA = String.valueOf(attributeMap.get("codeDesA"));
                        String codeDesB = String.valueOf(attributeMap.get("codeDesB"));
                        String codeDesC = String.valueOf(attributeMap.get("codeDesC"));
                        String layNo = String.valueOf(attributeMap.get("layNo"));
                        String layRec = String.valueOf(attributeMap.get("layRec"));
                        String fullPathName = String.valueOf(attributeMap.get("fullPathName"));
                        String parentId = String.valueOf(attributeMap.get("parentId"));
                        Long sort = null;
                        if (!attributeMap.get("sort").equals("")) {
                            sort = Long.valueOf(String.valueOf(attributeMap.get("sort")));
                        }
                        Boolean valid = Boolean.valueOf(String.valueOf(attributeMap.get("valid")));
                        Integer version = Integer.valueOf(String.valueOf(attributeMap.get("version")));

                        SystemEntity sysEntity1 = entityStack.peek();
                        String sysEntityCode = sysEntity1.getCode();

                        syscode.setEntityCode(sysEntityCode); // 从stack中拿到ENTITY_CODE，
                        syscode.setCompany(companyService.get(cid));
                        if (null != id)
                            syscode.setId(id);
                        if (null != code)
                            syscode.setCode(code);
                        if (null != value)
                            syscode.setValue(value);
                        syscode.setValueZhCn(internationalService.getI18nValue(value));
                        syscode.setName(value);
                        syscode.setDisplayName(syscode.getValueZhCn());
                        if (null != memo)
                            syscode.setMemo(memo);
                        if (null != sort)
                            syscode.setSort(sort);
                        if (null != valid)
                            syscode.setValid(valid);
                        if (null != version)
                            syscode.setVersion(version);
                        if (null != cid)
                            syscode.setCid(cid);
                        if (null != leaf)
                            syscode.setLeaf(leaf);
                        if (null != defaultFlag)
                            syscode.setDefaultFlag(defaultFlag);
                        if (null != layNo && !layNo.isEmpty())
                            syscode.setLayNo(Integer.valueOf(layNo));
                        if (null != codeDesA) {
                            syscode.setCodeDesA(codeDesA);
                        }
                        if (null != codeDesB) {
                            syscode.setCodeDesB(codeDesB);
                        }
                        if (null != codeDesC) {
                            syscode.setCodeDesC(codeDesC);
                        }
                        if (StringUtils.isNotEmpty(fullPathName)) {
                            syscode.setFullPathName(fullPathName);

                        }
                        if (StringUtils.isNotEmpty(parentId)) {
                            SystemCode parentSystemCode = parentId.contains("/") ? originStrSystemCodeMap.get(parentId) : originSystemCodeMap.get(Long.valueOf(parentId));
                            if (parentSystemCode != null && parentSystemCode.getPoId() != null) {
                                syscode.setParentId(parentSystemCode.getPoId().toString());
                                syscode.setParentName(parentSystemCode.getName());
                            }
                        }
                        if (StringUtils.isNotEmpty(layRec)) {
                            Long originId = null;
                            String[] layRecs = layRec.split("-");
                            StringBuilder layRecBuffer = new StringBuilder();
                            StringBuilder fullPathBuffer = new StringBuilder();
                            for (int i = 0; i < layRecs.length; i++) {
                                Long originLayRec = Long.valueOf(layRecs[i]);
                                if (i == layRecs.length - 1) {
                                    originId = originLayRec;
                                    originSystemCodeMap.put(originId, syscode);
                                    originStrSystemCodeMap.put(syscode.getId(), syscode);
                                }
                                SystemCode systemCode = originSystemCodeMap.get(originLayRec);
                                if (systemCode != null) {
                                    if (i > 0) {
                                        layRecBuffer.append("-");
                                        fullPathBuffer.append("/");
                                    }
                                    layRecBuffer.append(systemCode.getPoId());
                                    fullPathBuffer.append(systemCode.getCode());
                                }
                            }
                            syscode.setLayRec(layRecBuffer.toString());
                            syscode.setFullPath(fullPathBuffer.toString());
                        }
                        systemCodeList.add(syscode);
                    }
                    codeStack.push(syscode);
                }
            } else if (r.isEndElement()) {
                String en = r.getName().toString();
                if (en.equals("bapSystemCode")) {
                    break;
                } else {
                    if (en.equals("sysentity")) {
                        if (entityStack.size() > 0) {
                            entityStack.pop();
                        }
                    } else if (en.equals("systemcode")) {
                        if (codeStack.size() > 0) {
                            codeStack.pop();
                        }
                    }
                }
            }
        }

        // 排序，让父系统编码先保存，可以获取其id
        systemCodeList.sort(comparing(SystemCode::getLayNo));
        systemCodeList.forEach(s -> {
            log.info("start save systemcode: " + s.toString());
            saveSystemCode(s);
        });
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<SystemCode> getBySystemCodePage(Page<SystemCode> page, DetachedCriteria detachedCriteria) {
        detachedCriteria.addOrder(Order.asc("sort"));
        Page<SystemCode> pages = systemCodeDao.findByPage(page, detachedCriteria);
        List<SystemCode> list = pages.getResult();
        List<SystemCode> result = new ArrayList<SystemCode>();
        for (SystemCode s : list) {
            String value = s.getValue();
            String inter = InternationalResource.get(value);
            s.setValue(inter);
            result.add(s);
        }
        page.setResult(result);
        return page;//systemCodeDao.findByPage(page, detachedCriteria);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Set<SystemCode> getTreeList(Company company, String systemEntityCode, SystemCode systemcode) {
        SystemCode root = new SystemCode();
        List<SystemCode> lists = buildTree(prepareDataForTree(company, systemEntityCode));
        Set<SystemCode> list = new LinkedHashSet<>();
        for (SystemCode s : lists) {
            list.add(s);
        }
        root.setChildren(list);
        root.setId("-1");
        SystemCode tmp = new SystemCode();
        if (systemcode != null) {
            tmp.setId(systemcode.getId());
        } else {
            tmp.setId("-1");
        }
        return getSystemCodeTree(tmp.getId(), root).getChildren();
    }

    @Override
    public void deleteAndChildren(String id, Integer version) {
        // TODO Auto-generated method stub
        SystemCode systemCode = systemCodeDao.load(id);
        if (null != systemCode && null != systemCode.getParentId() && systemCode.getParentId().length() > 0) {
            String hql = "from SystemCode where valid=? and parentId=? and id!=?";
            List<SystemCode> list = systemCodeDao.findByHql(hql, true, systemCode.getParentId(), id);
            if (null == list || list.isEmpty()) {
                String updateHql = "update SystemCode set leaf=? where id=?";
                systemCodeDao.bulkExecute(updateHql, true, systemCode.getParentId());
            }
        }
        String queryHql = "from SystemCode where valid=? and layRec like ? ";
        List<SystemCode> deleteList = systemCodeDao.findByHql(queryHql, true, systemCode.getLayRec() + "%");
        systemCodeDao.deleteAll(deleteList);
        systemCodeDao.flush();
    }

    public List<SystemCode> buildTree(List<SystemCode> data) {
        List<SystemCode> trees = new ArrayList<SystemCode>();
        Map<String,Set<SystemCode>> treeChildrenMap=new HashMap<>();
        if (null != data) {
            Map<String, SystemCode> map = new HashMap<String, SystemCode>(data.size());
            for (SystemCode s : data) {
                map.put(s.getId(), s);
            }
            for (SystemCode node : data) {
                if (null == node.getParentId()) {
                    node.setChildren(treeChildrenMap.get(node.getId()));
                    trees.add(node);
                } else {
                    SystemCode parent = map.get(node.getParentId());
                    Set<SystemCode> children=treeChildrenMap.get(parent.getId());
                    if(null==children){
                        children=new LinkedHashSet<>() ;
                        children.add(node);
                        treeChildrenMap.put(parent.getId(),children);
                    }else{
                        children.add(node);
                    }
                    node.setChildren(treeChildrenMap.get(node.getId()));
                    node.setParent(parent);
                }
            }
        }
        return trees;
    }

    public List<SystemCode> prepareDataForTree(Company company, String systemEntityCode) {
        return systemCodeDao.createCriteria(Restrictions.eq("valid", true), Restrictions.eq("entityCode", systemEntityCode), Restrictions.or(Restrictions.or(Restrictions.eq("cid", 0L), Restrictions.eq("cid", 1L)), Restrictions.eq("cid", company.getId()))).addOrder(Order.asc("sort")).addOrder(Order.asc("id")).list();
    }

    private SystemCode getSystemCodeTree(String id, SystemCode tree) {
        if (null == id)
            return null;
        SystemCode subTree = getRescure(id, tree);
        return subTree;
    }

    private SystemCode getRescure(String id, SystemCode node) {
        if (id.equals(node.getId()))
            return node;
        SystemCode tree = null;
        if (null != node.getChildren() && !node.getChildren().isEmpty())
            for (SystemCode n : node.getChildren()) {
                tree = getRescure(id, n);
                if (null != tree)
                    break;
            }
        return tree;
    }
}
