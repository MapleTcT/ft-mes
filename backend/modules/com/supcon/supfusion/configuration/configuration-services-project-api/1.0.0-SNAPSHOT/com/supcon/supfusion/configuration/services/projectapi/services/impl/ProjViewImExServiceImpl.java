package com.supcon.supfusion.configuration.services.projectapi.services.impl;

import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.hibernate.BAPNamingStrategy;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.utils.Inflector;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjBuildTplService;
import com.supcon.supfusion.configuration.services.projectapi.services.ProjImportExportService;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import io.jsonwebtoken.lang.Assert;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.*;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.Transient;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/23
 */
@Slf4j
@Transactional
@ServiceApiService
public class ProjViewImExServiceImpl implements ProjImportExportService {
    private static final String FILE_NAME = "View.xml";
    private static final String DIR_NAME = "View";
    @Autowired
    private ProjBuildTplService projBuildTplService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private CustomerConditionService customerConditionService;
    @Autowired
    private ReflectService reflectService;
    @Autowired
    private ViewService viewService;
    @Autowired
    private EventService eventService;
    @Autowired
    private DataGridService dataGridService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private GenerateService generateService;

    @Autowired
    private MenuInfoService menuinfoService;
    @Autowired
    SessionFactory sessionFactory;

    @SuppressWarnings("rawtypes")
    private Map<String, Class> fieldTypeMap = new HashMap<String, Class>();
    private Set<String> errorFieldTypeMap = new HashSet<String>();

    @Override
    @Transactional(propagation = Propagation.REQUIRED, timeout = -1)
    public synchronized void importProjConfig(String moduleCode, String path) {
        File dir=new File(path+File.separator+DIR_NAME);
        if(!dir.exists()){
            return;
        }
        if(sessionFactory==null){
            /*Bundle bundle = FrameworkUtil.getBundle(getClass());
            if (bundle != null) {
                BundleContext context = bundle.getBundleContext();
                ServiceReference[] refs=null;
                try {
                    refs = context.getServiceReferences(SessionFactory.class.getName(), null);
                } catch (InvalidSyntaxException e) {
                    logger.error(e.getMessage(), e);
                }
                if(refs!=null){
                    for(ServiceReference ref:refs){
                        if("entityconfSessionFactory".equals(ref.getProperty("qualifier"))){
                            sessionFactory=(SessionFactory)context.getService(ref);
                        }
                    }
                }
            }*/
        }
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        List<Entity> entityList=modelService.getEntities(moduleCode);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        Map<String, List<Map<String, Object>>> metaMap=new HashMap<String, List<Map<String,Object>>>();
        metaMap.put(com.supcon.supfusion.configuration.services.entity.Field.class.getName(), new ArrayList<Map<String, Object>>());
        metaMap.put(Button.class.getName(), new ArrayList<Map<String, Object>>());
        metaMap.put(Validate.class.getName(), new ArrayList<Map<String, Object>>());
        metaMap.put(Event.class.getName(), new ArrayList<Map<String, Object>>());
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        for (Entity entity : entityList) {
            File f = new File(path + File.separator + DIR_NAME + File.separator + entity.getCode() + File.separator + FILE_NAME);
            if (!f.exists()) {
                continue;
            }
            try {
                importProjViewFromXML(FileUtils.readFileToString(f), entity, metaMap);
            } catch (IOException e) {
                throw new EcException(e);
            }
        }
        try {
            synchronizeObjInfo(metaMap, moduleCode,sessionFactory.getCurrentSession(),"runtime");
        } catch (Exception e) {

            throw new EcException(e);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
    }

    @Override
    @Transactional
    public void exportProjConfig(String moduleCode, String path) {
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
        List<Entity> entityList=modelService.getEntities(moduleCode);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        File dir=new File(path+File.separator+DIR_NAME);
        if(!dir.exists()){
            if (!dir.mkdirs()) {
                throw new EcException("can not make ProjConfig's dir.");
            }
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(true);
        if(sessionFactory==null){
           /* Bundle bundle = FrameworkUtil.getBundle(getClass());
            if (bundle != null) {
                BundleContext context = bundle.getBundleContext();
                ServiceReference[] refs=null;
                try {
                    refs = context.getServiceReferences(SessionFactory.class.getName(), null);
                } catch (InvalidSyntaxException e) {
                    logger.error(e.getMessage(), e);
                }
                if(refs!=null){
                    for(ServiceReference ref:refs){
                        if("entityconfSessionFactory".equals(ref.getProperty("qualifier"))){
                            sessionFactory=(SessionFactory)context.getService(ref);
                        }
                    }
                }
            }*/
        }
        Session statelessSession = null;
        try {
            statelessSession = this.sessionFactory.getCurrentSession();
            List<com.supcon.supfusion.configuration.services.entity.Field> fields = findByModuleCode(statelessSession, com.supcon.supfusion.configuration.services.entity.Field.class, moduleCode);
            List<com.supcon.supfusion.configuration.services.entity.Button> buttons = findByModuleCode(statelessSession, com.supcon.supfusion.configuration.services.entity.Button.class, moduleCode);
            List<com.supcon.supfusion.configuration.services.entity.Event> events = findByModuleCode(statelessSession, com.supcon.supfusion.configuration.services.entity.Event.class, moduleCode);
            List<CustomerCondition> cclist = customerConditionService.findCustomerConditionsByCode(moduleCode);
            for (Entity entity : entityList) {
                Map<String, Object> map = new HashMap<String, Object>();
                Map<String, List<DataGroup>> dataGroupMap = new HashMap<String, List<DataGroup>>();
                Page<View> pv = new Page<View>();
                pv.setAll(true);
                pv.setExportFlag(true);
                Page<View> views = viewService.findViews(pv, Restrictions.eq("entity", entity),
                        Restrictions.or(Restrictions.eq("mobile", Boolean.FALSE), Restrictions.isNull("mobile")), Restrictions.eq("valid", Boolean.TRUE),
                        Restrictions.eq("projFlag", Boolean.TRUE));
                List<View> viewList = views.getResult();
                List<Event> eventList = new ArrayList<Event>();
                if (viewList.size() > 0) {
                    for (View view : viewList) {
                        List<DataGrid> dgList = dataGridService.getDataGridByView(view, true);
                        view.setDataGrids(dgList);
                        dataGroupMap.put(view.getCode(), viewService.findDataGroups(view));
                        eventList.addAll(getProjViewEvent(view));
                        generateService.generateProjDataGridConfig(view, fields, buttons, events);
                    }
                }
                if (null != cclist && !cclist.isEmpty()) {
                    List<CustomerCondition> conditions = new ArrayList<CustomerCondition>();
                    for(CustomerCondition condition : cclist){
                        if(!StringUtils.isEmpty(condition.getEntityCode()) && condition.getEntityCode().equals(entity.getCode())){
                            conditions.add(condition);
                        }
                    }
                    map.put("customerConditions", conditions);
                }
                map.put("viewList", viewList);
                map.put("dataGroupMap", dataGroupMap);
                map.put("eventList", eventList);
                projBuildTplService.buildTpl("proj_view.ftl", path + File.separator + DIR_NAME + File.separator + entity.getCode(), FILE_NAME, map);
            }
        }catch (Exception e){
            throw new EcException(moduleCode+ InternationalResource.get("ec.project.upload.error1")+e.getMessage());
        }finally{
            /*if(statelessSession!=null){
                statelessSession.close();
            }*/
            ProjectFlagHolder.getInstance().getProjFlag().set(false);
        }
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
    }

    private <T> List<T> findByModuleCode(Session statelessSession, Class<T> clazz, String moduleCode) {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<T> result =  statelessSession.createCriteria(clazz).add(Restrictions.eq("moduleCode", moduleCode)).add(Restrictions.eq("valid", true)).addOrder(Order.asc("code")).setReadOnly(true).list();
        long end = System.currentTimeMillis();
        log.debug("Load {} casted {}ms.", clazz.getSimpleName(), (end - start));
        return result;
    }
    private void importProjViewFromXML(String xml,Entity entity,Map<String, List<Map<String, Object>>> metaMap){
        Document doc;

        String moduleCode=entity.getModule().getCode();
        String entityCode=entity.getCode();
        EcEnv ecEnv = null;
        List<Map<String, Object>> projEventsList = new ArrayList<>();
        Map<String,List<Map<String, Object>>> localMetaMap = new HashMap<String, List<Map<String,Object>>>();

        try {
            doc = DocumentHelper.parseText(xml);
            Element entityE = doc.getRootElement();
            for (Iterator subEntityIT = entityE.elementIterator(); subEntityIT.hasNext();) {
                Element subEntityE = (Element) subEntityIT.next();
                String subEntityName = subEntityE.getName();
                if ("views".equals(subEntityName)) {
                    for (Iterator viewsIt = subEntityE.elementIterator("view"); viewsIt.hasNext();) {
                        Map<String, Object> newView=addMetaDataItem(metaMap, View.class.getName());
                        newView.put("entity", entity);
                        Element viewE = (Element) viewsIt.next();
                        View view = new View();
                        view.setCode(XmlUtils.getTagContent(viewE.asXML(), "code"));
                        String viewEnv = XmlUtils.getTagContent(viewE.asXML(), "ecEnv");
                        if(ecEnv==null){
                            ecEnv = EcEnv.valueOf(viewEnv);
                        }
                        String projFlagStr = XmlUtils.getTagContent(viewE.asXML(), "projFlag");
                        Boolean projFlag = null;
                        if(null != projFlagStr &&!"".equals(projFlagStr)){
                            projFlag = "true".equals(projFlagStr)?true:false;
                        }
                        String inheritTypeStr = XmlUtils.getTagContent(viewE.asXML(), "inheritType");
                        if("1".equals(inheritTypeStr)){
                            projFlag = null;
                        }
                        view.setEcEnv((viewEnv != null && !viewEnv.isEmpty()) ? EcEnv.valueOf(viewEnv) : PropertyHolder.getEcEnv());
                        for (Iterator viewIt = viewE.elementIterator(); viewIt.hasNext();) {
                            Element viewPropertyE = (Element) viewIt.next();
                            String viewPropertyName=viewPropertyE.getName();
                            if ("reference".equals(viewPropertyName) || "shadowView".equals(viewPropertyName)
                                    || "batchControlPrintSelectView".equals(viewPropertyName)) {
                                // 含code
                                Element codeE = viewPropertyE.element("code");
                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                    View v = new View();
                                    v.setCode(codeE.getTextTrim());
                                    newView.put(viewPropertyName, v);
                                }
                            } else if ("assModel".equals(viewPropertyName)) {
                                // 含code
                                Element codeE = viewPropertyE.element("code");
                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                    Model assModel = new Model();
                                    assModel.setCode(codeE.getTextTrim());
                                    newView.put(viewPropertyName, assModel);
                                }
                            } else if ("extraView".equals(viewPropertyName)) {
                                // 含config
                                Map<String, Object> newExtraView=addMetaDataItem(metaMap, ExtraView.class.getName());
                                newExtraView.put("view", view);
                                newExtraView.put("code", view.getCode());
                                newExtraView.put("ecEnv", view.getEcEnv());
                                newExtraView.put("projFlag", projFlag);
                                Element codeE = viewPropertyE.element("config");
                                if (null != codeE) {
                                    Element viewJsonE = viewPropertyE.element("viewJson");
                                    codeE = codeE.element("config");
                                    if (codeE != null) {
                                        String fullConfig = codeE.asXML();
                                        newExtraView.put("config", fullConfig);
                                        newExtraView.put("fullConfig", fullConfig);
                                    }
                                    if(null !=viewJsonE){
                                        newExtraView.put("viewJson", viewJsonE.getStringValue());
                                    }
                                }
                                getLocalMetaDataItem(localMetaMap, ExtraView.class.getName()).add(newExtraView);
                            } else if ("fastQueryJson".equals(viewPropertyName)) {
                                Map<String, Object> newFastQueryJson=addMetaDataItem(metaMap, FastQueryJson.class.getName());
                                newFastQueryJson.put("view", view);
                                newFastQueryJson.put("code", view.getCode());
                                newFastQueryJson.put("ecEnv", view.getEcEnv());
                                newFastQueryJson.put("projFlag", projFlag);
                                // 含queryConfig
                                Element codeE = viewPropertyE.element("queryConfig");
                                if (null != codeE) {
                                    codeE = codeE.element("config");
                                    if (codeE != null) {
                                        newFastQueryJson.put("queryConfig", codeE.asXML());
                                    }
                                }
                                getLocalMetaDataItem(localMetaMap, FastQueryJson.class.getName()).add(newFastQueryJson);
                            } else if ("defaultAdvCond".equals(viewPropertyName)) {
                                Map<String, Object> newDefaultAdvCond=addMetaDataItem(metaMap, DefaultAdvCond.class.getName());
                                newDefaultAdvCond.put("viewCode", view.getCode());
                                newDefaultAdvCond.put("ecEnv", view.getEcEnv());
                                // 含content
                                for (Iterator defaultAdvCondIt = viewPropertyE.elementIterator(); defaultAdvCondIt.hasNext();) {
                                    Element defaultAdvCondPropertyE = (Element) defaultAdvCondIt.next();
                                    String defaultAdvCondPropertyName = defaultAdvCondPropertyE.getName();
                                    String defaultAdvCondPropertyValue = defaultAdvCondPropertyE.getTextTrim();
                                    if (defaultAdvCondPropertyValue.length() > 0) {
                                        newDefaultAdvCond.put(defaultAdvCondPropertyName,
                                                getObjectValue(DefaultAdvCond.class, defaultAdvCondPropertyName, defaultAdvCondPropertyValue));
                                    }
                                }
                            } else if ("extraQueryJson".equals(viewPropertyName)) {
                                Map<String, Object> newExtraQueryJson=addMetaDataItem(metaMap, ExtraQueryJson.class.getName());
                                newExtraQueryJson.put("view", view);
                                newExtraQueryJson.put("code", view.getCode());
                                newExtraQueryJson.put("ecEnv", view.getEcEnv());
                                newExtraQueryJson.put("projFlag", projFlag);
                                // 含queryConfig
                                Element codeE = viewPropertyE.element("queryConfig");
                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                    newExtraQueryJson.put("queryConfig", codeE.getTextTrim());
                                }
                            } else if ("sqls".equals(viewPropertyName)) {
                                for (Iterator sqlsIt = viewPropertyE.elementIterator("sql"); sqlsIt.hasNext();) {
                                    Element sqlE = (Element) sqlsIt.next();
                                    Map<String, Object> newSql=addMetaDataItem(metaMap, Sql.class.getName());
                                    newSql.put("projFlag", projFlag);
                                    for (Iterator sqlIt = sqlE.elementIterator(); sqlIt.hasNext();) {
                                        Element sqlPropertyE = (Element) sqlIt.next();
                                        String sqlPropertyName = sqlPropertyE.getName();
                                        String sqlPropertyValue = sqlPropertyE.getTextTrim();
                                        if (sqlPropertyValue.length() > 0) {
                                            newSql.put(sqlPropertyName,getObjectValue(Sql.class, sqlPropertyName, sqlPropertyValue));
                                        }
                                    }
                                }
                            } else if ("dataGrids".equals(viewPropertyName)) {
                                for (Iterator dataGridsIt = viewPropertyE.elementIterator("dataGrid"); dataGridsIt.hasNext();) {
                                    Element dataGridE = (Element) dataGridsIt.next();
                                    Map<String, Object> newDataGrid=addMetaDataItem(metaMap, DataGrid.class.getName());
                                    newDataGrid.put("view", view);
                                    newDataGrid.put("projFlag", projFlag);
                                    for (Iterator dataGridIt = dataGridE.elementIterator(); dataGridIt.hasNext();) {
                                        Element dataGridPropertyE = (Element) dataGridIt.next();
                                        if(dataGridPropertyE != null){
                                            String dataGridPropertyName = dataGridPropertyE.getName();
                                            if ("targetModel".equals(dataGridPropertyName)) {
                                                Element codeE = dataGridPropertyE.element("code");
                                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                                    Model m = new Model();
                                                    m.setCode(codeE.getTextTrim());
                                                    newDataGrid.put(dataGridPropertyName, m);
                                                }
                                            } else if ("orgProperty".equals(dataGridPropertyName)) {
                                                Element codeE = dataGridPropertyE.element("code");
                                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                                    Property property = new Property();
                                                    property.setCode(codeE.getTextTrim());
                                                    newDataGrid.put(dataGridPropertyName, property);
                                                }
                                            } else if ("config".equals(dataGridPropertyName) && null != dataGridPropertyE) {
                                                Element codeE = dataGridPropertyE;
                                                if (null != codeE) {
                                                    codeE = codeE.element("config");
                                                    if (codeE != null) {
                                                        String fullConfig = codeE.asXML();
                                                        newDataGrid.put("config", fullConfig);
                                                        newDataGrid.put("fullConfig", fullConfig);
                                                    }
                                                }
                                            } else {
                                                String dataGridPropertyValue = dataGridPropertyE.getTextTrim();
                                                if (dataGridPropertyValue.length() > 0) {
                                                    newDataGrid.put(dataGridPropertyName,getObjectValue(DataGrid.class, dataGridPropertyName, dataGridPropertyValue));
                                                }
                                            }
                                        }
                                    }
                                    if (null == newDataGrid.get("moduleCode")) {
                                        newDataGrid.put("moduleCode",moduleCode);
                                        newDataGrid.put("entityCode",entityCode);
                                    }
                                    getLocalMetaDataItem(localMetaMap, DataGrid.class.getName()).add(newDataGrid);
                                }
                            } else if ("dataGroups".equals(viewPropertyName)) {
                                for (Iterator dataGroupsIt = viewPropertyE.elementIterator("dataGroup"); dataGroupsIt.hasNext();) {
                                    Element dataGroupE = (Element) dataGroupsIt.next();
                                    Map<String, Object> newDataGroup=addMetaDataItem(metaMap, DataGroup.class.getName());
                                    newDataGroup.put("view", view);
                                    DataGroup dataGroup = new DataGroup();
                                    dataGroup.setCode(XmlUtils.getTagContent(dataGroupE.asXML(), "code"));
                                    newDataGroup.put("projFlag", projFlag);
                                    for (Iterator dataGroupIt = dataGroupE.elementIterator(); dataGroupIt.hasNext();) {
                                        Element dataGroupPropertyE = (Element) dataGroupIt.next();
                                        String dataGroupPropertyName = dataGroupPropertyE.getName();
                                        if ("dataClassifics".equals(dataGroupPropertyName)) {
                                            for (Iterator dataClassificsIt = dataGroupPropertyE.elementIterator("dataClassific"); dataClassificsIt
                                                    .hasNext();) {
                                                Element dataClassificE = (Element) dataClassificsIt.next();
                                                Map<String, Object> newDataClassific=addMetaDataItem(metaMap, DataClassific.class.getName());
                                                newDataClassific.put("dataGroup", dataGroup);
                                                for (Iterator dataClassificIt = dataClassificE.elementIterator(); dataClassificIt.hasNext();) {
                                                    Element dataClassificPropertyE = (Element) dataClassificIt.next();
                                                    String dataClassificPropertyName = dataClassificPropertyE.getName();
                                                    String dataClassificPropertyValue = dataClassificPropertyE.getTextTrim();
                                                    if (dataClassificPropertyValue.length() > 0) {
                                                        newDataClassific.put(dataClassificPropertyName,
                                                                getObjectValue(DataClassific.class, dataClassificPropertyName,dataClassificPropertyValue));
                                                    }
                                                }
                                                if (null == newDataClassific.get("moduleCode")) {
                                                    newDataClassific.put("moduleCode", moduleCode);
                                                    newDataClassific.put("entityCode", entityCode);
                                                }
                                                newDataClassific.put("projFlag", projFlag);
                                            }
                                        } else if ("targetModel".equals(dataGroupPropertyName)){
                                            Element codeE = dataGroupPropertyE.element("code");
                                            if (null != codeE && codeE.getTextTrim().length() > 0) {
                                                Model m = new Model();
                                                m.setCode(codeE.getTextTrim());
                                                newDataGroup.put(dataGroupPropertyName, m);
                                            }
                                        } else {
                                            String dataGroupPropertyValue = dataGroupPropertyE.getTextTrim();
                                            if (dataGroupPropertyValue.length() > 0) {
                                                newDataGroup.put(dataGroupPropertyName,
                                                        getObjectValue(DataGroup.class, dataGroupPropertyName, dataGroupPropertyValue));
                                            }
                                        }
                                    }
                                    if (null == newDataGroup.get("moduleCode")) {
                                        newDataGroup.put("moduleCode", moduleCode);
                                        newDataGroup.put("entityCode", entityCode);
                                    }
                                }
                            } else if("fastQueryJsons".equals(viewPropertyName)){
                                for (Iterator fastQueryJsons = viewPropertyE.elementIterator("fastQueryJson"); fastQueryJsons.hasNext();) {
                                    Element fastQueryJsonE = (Element) fastQueryJsons.next();
                                    Map<String, Object> newFastQueryJson=addMetaDataItem(metaMap, FastQueryJson.class.getName());
                                    newFastQueryJson.put("view", view);
                                    FastQueryJson fastQueryJson = new FastQueryJson();
                                    fastQueryJson.setCode(XmlUtils.getTagContent(fastQueryJsonE.asXML(), "code"));
                                    newFastQueryJson.put("projFlag", projFlag);
                                    for (Iterator fastQueryJsonIt = fastQueryJsonE.elementIterator(); fastQueryJsonIt.hasNext();) {
                                        Element fastQueryJsonPropertyE = (Element) fastQueryJsonIt.next();
                                        if(null !=fastQueryJsonPropertyE){
                                            String fastQueryJsonPropertyName = fastQueryJsonPropertyE.getName();
                                            if ("queryConfig".equals(fastQueryJsonPropertyName) && null != fastQueryJsonPropertyName) {
                                                Element codeE = fastQueryJsonPropertyE;
                                                if (null != codeE) {
                                                    codeE = codeE.element("config");
                                                    if (codeE != null) {
                                                        String fullConfig = codeE.asXML();
                                                        newFastQueryJson.put("queryConfig", fullConfig);
                                                    }
                                                }
                                            } else if ("targetModel".equals(fastQueryJsonPropertyName)){
                                                Element codeE = fastQueryJsonPropertyE.element("code");
                                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                                    Model m = new Model();
                                                    m.setCode(codeE.getTextTrim());
                                                    newFastQueryJson.put(fastQueryJsonPropertyName, m);
                                                }
                                            } else {
                                                String fastQueryJsonPropertyValue = fastQueryJsonPropertyE.getTextTrim();
                                                if (fastQueryJsonPropertyValue.length() > 0) {
                                                    newFastQueryJson.put(fastQueryJsonPropertyName,getObjectValue(FastQueryJson.class, fastQueryJsonPropertyName, fastQueryJsonPropertyValue));
                                                }
                                            }
                                        }
                                    }
                                    getLocalMetaDataItem(localMetaMap, FastQueryJson.class.getName()).add(newFastQueryJson);
                                }
                            } else if("advQueryJsons".equals(viewPropertyName)){
                                for (Iterator advQueryJsons = viewPropertyE.elementIterator("advQueryJson"); advQueryJsons.hasNext();) {
                                    Element advQueryJsonE = (Element) advQueryJsons.next();
                                    Map<String, Object> newAdvQueryJson=addMetaDataItem(metaMap, AdvQueryJson.class.getName());
                                    newAdvQueryJson.put("view", view);
                                    AdvQueryJson advQueryJson = new AdvQueryJson();
                                    advQueryJson.setCode(XmlUtils.getTagContent(advQueryJsonE.asXML(), "code"));
                                    newAdvQueryJson.put("projFlag", projFlag);
                                    for (Iterator advQueryJsonIt = advQueryJsonE.elementIterator(); advQueryJsonIt.hasNext();) {
                                        Element advQueryJsonPropertyE = (Element) advQueryJsonIt.next();
                                        if(null != advQueryJsonPropertyE){
                                            String advQueryJsonPropertyName = advQueryJsonPropertyE.getName();
                                            if ("queryConfig".equals(advQueryJsonPropertyName) && null != advQueryJsonPropertyName) {
                                                Element codeE = advQueryJsonPropertyE;
                                                if (null != codeE) {
                                                    codeE = codeE.element("config");
                                                    if (codeE != null) {
                                                        String fullConfig = codeE.asXML();
                                                        newAdvQueryJson.put("queryConfig", fullConfig);
                                                    }
                                                }
                                            } else if ("targetModel".equals(advQueryJsonPropertyName)){
                                                Element codeE = advQueryJsonPropertyE.element("code");
                                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                                    Model m = new Model();
                                                    m.setCode(codeE.getTextTrim());
                                                    newAdvQueryJson.put(advQueryJsonPropertyName, m);
                                                }
                                            } else {
                                                String advQueryJsonPropertyValue = advQueryJsonPropertyE.getTextTrim();
                                                if (advQueryJsonPropertyValue.length() > 0) {
                                                    newAdvQueryJson.put(advQueryJsonPropertyName,getObjectValue(AdvQueryJson.class, advQueryJsonPropertyName, advQueryJsonPropertyValue));
                                                }
                                            }}
                                    }
                                    getLocalMetaDataItem(localMetaMap, AdvQueryJson.class.getName()).add(newAdvQueryJson);
                                }
                            } else {
                                // View simple property
                                String viewPropertyValue = viewPropertyE.getTextTrim();
                                if("parentMenuCode".equals(viewPropertyName)){
                                    String parentMenuCode = viewPropertyE.getTextTrim();
                                    if(parentMenuCode!=null&&parentMenuCode.length()>0){
                                        MenuInfo mi=menuinfoService.getMenuInfo(parentMenuCode);
                                        if(mi!=null){
                                            newView.put("parentMenuId",mi.getId());
                                        }
                                    }
                                }
                                if (viewPropertyValue.length() > 0) {
                                    newView.put(viewPropertyName,getObjectValue(View.class, viewPropertyName, viewPropertyValue));
                                }
                            }
                        }
                    }
                }else if ("customerConditions".equals(subEntityName)) {
                    for (Iterator conditionsIt = subEntityE.elementIterator("customerCondition"); conditionsIt.hasNext();) {
                        Element conditionE = (Element) conditionsIt.next();
                        Map<String, Object> newCustomerCondition=addMetaDataItem(metaMap, CustomerCondition.class.getName());
                        newCustomerCondition.put("moduleCode", moduleCode);
                        newCustomerCondition.put("entityCode", entityCode);
                        for (Iterator conditionIt = conditionE.elementIterator(); conditionIt.hasNext();) {
                            Element conditionPropertyE = (Element) conditionIt.next();
                            String conditionPropertyName = conditionPropertyE.getName();
                            if (conditionPropertyName.equals("view")) {
                                Element codeE = conditionPropertyE.element("code");
                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                    View view = new View();
                                    view.setCode(codeE.getTextTrim());
                                    newCustomerCondition.put("view", view);
                                }
                            } else if (conditionPropertyName.equals("dataGrid")) {
                                Element codeE = conditionPropertyE.element("code");
                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                    DataGrid dataGrid = new DataGrid();
                                    dataGrid.setCode(codeE.getTextTrim());
                                    newCustomerCondition.put("dataGrid", dataGrid);
                                }
                            } else if (conditionPropertyName.equals("dataClassific")) {
                                Element codeE = conditionPropertyE.element("code");
                                if (null != codeE && codeE.getTextTrim().length() > 0) {
                                    DataClassific classific = new DataClassific();
                                    classific.setCode(codeE.getTextTrim());
                                    newCustomerCondition.put("dataClassific", classific);
                                }
                            } else {
                                String conditionPropertyValue = conditionPropertyE.getTextTrim();
                                if (conditionPropertyValue.length() > 0) {
                                    newCustomerCondition.put(conditionPropertyName,
                                            getObjectValue(CustomerCondition.class, conditionPropertyName, conditionPropertyValue));
                                }
                            }
                        }
                    }
                }else if ("projEvents".equals(subEntityName)) {
                    for (Iterator eventsIt = subEntityE.elementIterator("event"); eventsIt.hasNext();) {
                        Element eventE = (Element) eventsIt.next();
                        Map<String, Object> newEvent=new HashMap<String, Object>();
                        newEvent.put("moduleCode", moduleCode);
                        newEvent.put("entityCode", entityCode);
                        for (Iterator eventIt = eventE.elementIterator(); eventIt.hasNext();) {
                            Element eventPropertyE = (Element) eventIt.next();
                            String eventPropertyName = eventPropertyE.getName();
                            String eventPropertyValue;
                            if(null != eventPropertyName && "function".equals(eventPropertyName)){	//修复函数会自动截掉换行
                                eventPropertyValue = eventPropertyE.getText();
                            }else{
                                eventPropertyValue = eventPropertyE.getTextTrim();
                            }
                            if (eventPropertyValue.length() > 0) {
                                newEvent.put(eventPropertyName,getObjectValue(Event.class, eventPropertyName, eventPropertyValue));
                            }
                        }
                        projEventsList.add(newEvent);
                    }
                }
            }
            // 处理页面field、event、validate
            // 处理extraView与datagrid中的field
            List<Map<String, Object>> eventsList = new ArrayList<>();
            eventsList.addAll(projEventsList);
            List<Map<String, Object>> buttonsList = new ArrayList<>();
            List<Map<String, Object>> validateList = new ArrayList<>();
            List<Map<String, Object>> fieldsList = new ArrayList<>();
            List<Map<String, Object>> values = localMetaMap.get(ExtraView.class.getName());
            EcExtraViewIntegrationUtilsForUpload utils = new EcExtraViewIntegrationUtilsForUpload();
            if (ecEnv == null) {
                ecEnv = PropertyHolder.getEcEnv();
            }
            if(values!=null){
                for (Map<String, Object> mapItem : values) {
                    String viewCode = (String) mapItem.get("code");
                    utils.ecSplitConfig(viewCode, (String) mapItem.get("config"), moduleCode, entityCode, ecEnv, false);
                    eventsList.addAll(utils.getEventsList());
                    buttonsList.addAll(utils.getButtonsList());
                    validateList.addAll(utils.getValidateList());
                    fieldsList.addAll(utils.getFieldsList());
                }
            }
            values = localMetaMap.get(DataGrid.class.getName());
            if(values!=null){
                for (Map<String, Object> mapItem : values) {
                    String dgCode = (String) mapItem.get("code");
                    utils.ecSplitConfig(dgCode, (String) mapItem.get("config"), moduleCode, entityCode, ecEnv, false);
                    eventsList.addAll(utils.getEventsList());
                    buttonsList.addAll(utils.getButtonsList());
                    validateList.addAll(utils.getValidateList());
                    fieldsList.addAll(utils.getFieldsList());
                }
            }
            values = localMetaMap.get(FastQueryJson.class.getName());
            if(values!=null){
                for (Map<String, Object> mapItem : values) {
                    String fqjCode = (String) mapItem.get("code");
                    utils.ecSplitConfig(fqjCode, (String) mapItem.get("config"), moduleCode, entityCode, ecEnv, false);
                    eventsList.addAll(utils.getEventsList());
                    validateList.addAll(utils.getValidateList());
                    fieldsList.addAll(utils.getFieldsList());
                }
            }
            values = localMetaMap.get(AdvQueryJson.class.getName());
            if(values!=null){
                for (Map<String, Object> mapItem : values) {
                    String aqjCode = (String) mapItem.get("code");
                    utils.ecSplitConfig(aqjCode, (String) mapItem.get("config"), moduleCode, entityCode, ecEnv, false);
                    eventsList.addAll(utils.getEventsList());
                    validateList.addAll(utils.getValidateList());
                    fieldsList.addAll(utils.getFieldsList());
                }
            }
            List<Map<String, Object>> tmpList = new ArrayList<>();
            Map<String, Object> tmpMap = null;
            for (Map<String, Object> listItem : fieldsList) {
                tmpMap = new HashMap<>();
                tmpList.add(tmpMap);
                listItem.put("projFlag", true);
                for (Iterator<Map.Entry<String, Object>> it = listItem.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Object> mapItem = it.next();
                    // map中有许多field没有的字段，但是需要保存到config中去，去掉这些字段
                    if (checkIfPropertyExists(com.supcon.supfusion.configuration.services.entity.Field.class, mapItem.getKey())) {
                        Object tmpObj = mapItem.getValue();
                        if (tmpObj instanceof String) {
                            try {
                                tmpObj = getObjectValue(com.supcon.supfusion.configuration.services.entity.Field.class, mapItem.getKey(), (String) tmpObj);
                                tmpMap.put(mapItem.getKey(), tmpObj);
                            } catch (Exception e) {
                                log.warn(e.getMessage());
                            }
                        } else {
                            tmpMap.put(mapItem.getKey(), tmpObj);
                        }
                        // 过滤对象,存到field的config中，转换不了
                        if (tmpObj instanceof AbstractCodeEntity) {
                            it.remove();
                        }
                    }
                }
                tmpMap.put("field", listItem);
            }
            fieldsList = tmpList;

            tmpList = new ArrayList<>();
            for (Map<String, Object> listItem : buttonsList) {
                tmpMap = new HashMap<>();
                tmpList.add(tmpMap);
                listItem.put("projFlag", true);
                for (Iterator<Map.Entry<String, Object>> it = listItem.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Object> mapItem = it.next();
                    // map中有许多button没有的字段，但是需要保存到config中去，去掉这些字段
                    if (checkIfPropertyExists(Button.class, mapItem.getKey())) {
                        Object tmpObj = mapItem.getValue();
                        if (tmpObj instanceof String) {
                            try {
                                tmpObj = getObjectValue(Button.class, mapItem.getKey(), (String) tmpObj);
                                tmpMap.put(mapItem.getKey(), tmpObj);
                            } catch (Exception e) {
                                log.warn(e.getMessage());
                            }
                        } else {
                            tmpMap.put(mapItem.getKey(), tmpObj);
                        }
                        // 过滤对象,存到button的config中，转换不了
                        /*if (tmpObj instanceof EcCodeEntity) {
                            it.remove();
                        }*/
                    }
                }
                tmpMap.put("button", listItem);
            }
            buttonsList = tmpList;

            for (Map<String, Object> listItem : validateList) {
                listItem.put("projFlag", true);
                for (Map.Entry<String, Object> mapItem : listItem.entrySet()) {
                    if (mapItem.getValue() instanceof String) {
                        mapItem.setValue(getObjectValue(Validate.class, mapItem.getKey(), (String) mapItem.getValue()));
                    }
                }
            }
            for (Map<String, Object> listItem : eventsList) {
                listItem.put("projFlag", true);
                for (Map.Entry<String, Object> mapItem : listItem.entrySet()) {
                    if (mapItem.getValue() instanceof String) {
                        mapItem.setValue(getObjectValue(Event.class, mapItem.getKey(), (String) mapItem.getValue()));
                    }
                }
            }
            metaMap.get(com.supcon.supfusion.configuration.services.entity.Field.class.getName()).addAll(fieldsList);
            metaMap.get(Button.class.getName()).addAll(buttonsList);
            metaMap.get(Validate.class.getName()).addAll(validateList);
            metaMap.get(Event.class.getName()).addAll(eventsList);
        }  catch (Exception e) {
            throw new EcException(e);
        }

    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object getObjectValue(Class clazz, String name, String val) throws Exception {
        Class type = fieldTypeMap.get(clazz.getName() + "$$" + name);
        if (type == null) {
            Field field = getField(clazz, name);
            type = field.getType();
            fieldTypeMap.put(clazz.getName() + "$$" + name, type);
        }
        if (type.isEnum())
            return Enum.valueOf(type, val);
        return ConvertUtils.convert(val, type);
    }

    public Field getField(Class<?> clazz, String fieldName) throws IllegalStateException {
        Assert.notNull(clazz, "Class required");
        Assert.hasText(fieldName, "Field name required");
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException nsf) {
            // Try superclass
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            }

            throw new IllegalStateException("Could not locate field '" + fieldName + "' on class " + clazz);
        }
    }

    private Map<String, Object> addMetaDataItem(Map<String, List<Map<String, Object>>> metaMap, String typeName) {
        List<Map<String, Object>> ret = metaMap.get(typeName);
        if (ret == null) {
            ret = new ArrayList<Map<String, Object>>();
            metaMap.put(typeName, ret);
        }
        ret.add(new HashMap<String, Object>());
        return ret.get(ret.size() - 1);
    }

    private List<Map<String, Object>> getLocalMetaDataItem(Map<String, List<Map<String, Object>>> metaMap, String typeName) {
        List<Map<String, Object>> ret = metaMap.get(typeName);
        if (ret == null) {
            ret = new ArrayList<Map<String, Object>>();
            metaMap.put(typeName, ret);
        }
        return metaMap.get(typeName);
    }

    @SuppressWarnings("rawtypes")
    private boolean checkIfPropertyExists(Class clazz, String name) throws Exception {
        try {
            if (errorFieldTypeMap.contains(clazz.getName() + "$$" + name)) {
                return false;
            }
            Class type = fieldTypeMap.get(clazz.getName() + "$$" + name);
            if (type == null) {
                Field field = getField(clazz, name);
                type = field.getType();
                fieldTypeMap.put(clazz.getName() + "$$" + name, type);
            }
        } catch (Exception e) {
            errorFieldTypeMap.add(clazz.getName() + "$$" + name);
            return false;
        }
        return true;
    }

    /**
     * 同步数据
     *
     * @param
     *
     * @param moduleCode
     * @throws Exception
     */
    private void synchronizeObjInfo(Map<String, List<Map<String, Object>>> metaMap, String moduleCode, Session session,String... env) throws Exception {
//		List<String> updateRelationHQLs = new ArrayList<>();
//		List<List<Object>> updateRelationParam = new ArrayList<>();
        Map<String, Map<String, Object>> metaInfo = fetchMetaInfoInModule(moduleCode, session);
        // TODO 将来再完善(处理field)
        Map<String, Object> existsFields = metaInfo.get(com.supcon.supfusion.configuration.services.entity.Field.class.getName());
        List<Map<String, Object>> uploadViews = metaMap.get(View.class.getName());
        List<Map<String, Object>> uploadDataGrids = metaMap.get(DataGrid.class.getName());
        Set<String> uploadViewCodes = new HashSet<>();
        if(uploadViews!=null){
            for (Map<String, Object> item : uploadViews) {
                uploadViewCodes.add((String) item.get("code"));
            }
        }
        Set<String> uploadDataGridCodes = new HashSet<>();
        if(uploadDataGrids!=null){
            for (Map<String, Object> item : uploadDataGrids) {
                uploadDataGridCodes.add((String) item.get("code"));
            }
        }
        Set<String> fields2Del = new HashSet<>();
        for (Iterator<Map.Entry<String, Object>> it = existsFields.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Object> entry = it.next();
            com.supcon.supfusion.configuration.services.entity.Field f = (com.supcon.supfusion.configuration.services.entity.Field) entry.getValue();
            boolean existsFlag = false;
            for (Map<String, Object> fieldMap : metaMap.get(com.supcon.supfusion.configuration.services.entity.Field.class.getName())) {
                if (f.getCellCode() != null && f.getCellCode().equals(fieldMap.get("cellCode"))) {
                    existsFlag = true;
                    if (f.getCode() != null && !f.getCode().equals(fieldMap.get("code"))) {
                        // cellcode相同，fieldcode不同，必须要把原来的删除掉，不能在一个cell中放两个field
                        if (!fields2Del.contains(f.getCode())) {
                            fields2Del.add(f.getCode());
                            it.remove();
                        }
                    }
                    break;
                }
            }
            if (!existsFlag) {
                // 若不存在，并且field所属的视图或者pt在上载的包中，删除掉field
                if (!fields2Del.contains(f.getCode())) {
                    if (f.getView() != null && uploadViewCodes.contains(f.getView().getCode())) {
                        fields2Del.add(f.getCode());
                        it.remove();
                    } else if (f.getDataGrid() != null && uploadViewCodes.contains(f.getDataGrid().getCode())) {
                        fields2Del.add(f.getCode());
                        it.remove();
                    }
                }
            }
        }
        if (fields2Del != null && !fields2Del.isEmpty()) {
            List<List<String>> args = new ArrayList<>();
            Iterator<String> it = fields2Del.iterator();
            List<String> item = null;
            List<String> whereSqls = new ArrayList<>();
            List<String> whereSqlsField = new ArrayList<>();
            for (int i = 0; i < fields2Del.size() && it.hasNext(); i++) {
                if (i % 999 == 0) {
                    item = new ArrayList<>();
                    args.add(item);
                    whereSqls.add("field.code in(:fields2Del" + args.size() + ")");
                    whereSqlsField.add("code in(:fields2Del" + args.size() + ")");
                }
                item.add(it.next());
            }
            String whereSql = "";
            String whereSqlField = "";
            for (int i = 0; i < whereSqls.size(); i++) {
                if (i > 0) {
                    whereSql += " or ";
                    whereSqlField += " or ";
                }
                whereSql += whereSqls.get(i);
                whereSqlField += whereSqlsField.get(i);
            }

            String delValidateHql = "delete from Validate where " + whereSql;
            String delEventHql = "delete from Event where " + whereSql;
            String delFieldHql = "delete from Field where " + whereSqlField;
            Query query = createQuery(session, delValidateHql);
            for (int i = 0; i < whereSqls.size(); i++) {
                query.setParameterList("fields2Del" + (i + 1), args.get(i));
            }
            query.executeUpdate();

            query = createQuery(session, delEventHql);
            for (int i = 0; i < whereSqls.size(); i++) {
                query.setParameterList("fields2Del" + (i + 1), args.get(i));
            }
            query.executeUpdate();

            query = createQuery(session, delFieldHql);
            for (int i = 0; i < whereSqlsField.size(); i++) {
                query.setParameterList("fields2Del" + (i + 1), args.get(i));
            }
            query.executeUpdate();

            session.flush();
            session.clear();
        }
        for (Map.Entry<String, List<Map<String, Object>>> entryItem : metaMap.entrySet()) {
            Map<String, Map<String, Object>> inserts = new HashMap<>();
            Map<String, Map<String, Object>> updates = new HashMap<>();
            String entityName = entryItem.getKey();
            List<Map<String, Object>> values = entryItem.getValue();
            Map<String, Object> existsInDB = metaInfo.get(entityName);
            for (Map<String, Object> item : values) {
                String code = (String) item.get("code");
                if (existsInDB != null && existsInDB.containsKey(code)) {
                    updates.put(code, item);
                } else {
                    inserts.put(code, item);
                }
            }
            values.clear();
            boolean isEc = false;
            if(null != env && env[0].equalsIgnoreCase("ec")){
                isEc = true;
            }
            if (inserts != null && !inserts.isEmpty()) {
                for (Map.Entry<String, Map<String, Object>> entry : inserts.entrySet()) {
                    insertObject(Class.forName(entityName), entry.getValue(), isEc);
                }
                session.flush();
            }
            inserts.clear();
            if (updates != null && !updates.isEmpty()) {
                for (Map.Entry<String, Map<String, Object>> entry : updates.entrySet()) {
                    updateObject(Class.forName(entityName), entry.getValue(), isEc);
                }
                session.flush();
            }
            updates.clear();
        }
    }

    /**
     *
     * @param
     * @return
     */
    private Map<String, Map<String, Object>> fetchMetaInfoInModule(String moduleCode, Session session) {
        Map<String, Map<String, Object>> retMap = new HashMap<>();
        Map<String, Object> tmpMap = new HashMap<>();
        Criterion criterion = Restrictions.or(Restrictions.like("code", moduleCode, MatchMode.START),
                Restrictions.like("code", "_" + moduleCode + "_", MatchMode.ANYWHERE));
        for (EcEntityEnum enumItem : EcEntityEnum.values()) {
            tmpMap = fetchMetaInfo(session, enumItem.clazz, criterion);
            retMap.put(enumItem.clazz.getName(), tmpMap);
        }
        return retMap;
    }

    public enum EcEntityEnum {
        View(View.class), // 视图
        ExtraView(ExtraView.class), // ExtraView
        DataGrid(DataGrid.class), // DataGrid
        ExtraQueryJson(ExtraQueryJson.class), // 1对多快速查询
        DefaultAdvCond(DefaultAdvCond.class), //
        FastQueryJson(FastQueryJson.class), // 快速查询
        AdvQueryJson(AdvQueryJson.class), // 高级查询
        Button(Button.class), // 按钮
        CustomerCondition(CustomerCondition.class), // 自定义条件
        DataGroup(DataGroup.class), // 数据分组
        DataClassific(DataClassific.class), // 数据分类
        Field(com.supcon.supfusion.configuration.services.entity.Field.class), // 页面字段
        Validate(Validate.class), // 验证
        Sql(Sql.class), // SQL语句
        Event(Event.class);// 事件

        private Class<? extends AbstractCodeEntity> clazz;

        private EcEntityEnum(Class<? extends AbstractCodeEntity> clazz) {
            this.clazz = clazz;
        }

        @SuppressWarnings("unused")
        public Class<? extends AbstractCodeEntity> getClazz() {
            return clazz;
        }
    }
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchMetaInfo(Session session, Class<? extends AbstractCodeEntity> clazz, Criterion... criterions) {
        Map<String, Object> retMap = new HashMap<>();
        Criteria criteria = session.createCriteria(clazz);
        if (criterions != null && criterions.length > 0) {
            for (Criterion criterion : criterions) {
                criteria.add(criterion);
            }
        }
        List<? extends AbstractCodeEntity> result = criteria.list();
        if (result != null && !result.isEmpty()) {
            for (Object item : result) {
                AbstractCodeEntity tmp = (AbstractCodeEntity) item;
                retMap.put(tmp.getCode(), item);
            }
        }
        return retMap;
    }

    public Query createQuery(Session session, String hql, Object... objects) {
        Query query = session.createQuery(hql);
        if (objects != null)
            for (int i = 0; i < objects.length; i++)
                query.setParameter(i, objects[i]);
        return query;
    }

    /**
     * 插入对象
     *
     * @param entityClass
     *            类
     * @param obj
     *            要插入的对象
     * @param isEc
     *            是否向EC表插入
     * @throws Exception
     */
    private void insertObject(Class<?> entityClass, Map<String, Object> obj, Boolean isEc) throws Exception {
        if (obj == null || obj.size() == 0) {
            return;
        }
        // setObject
        Map<String, Object> tmp = new HashMap<>();
        if (entityClass.equals(com.supcon.supfusion.configuration.services.entity.Field.class)) {
            tmp.put("field", obj.get("field"));
            obj.put("config", SerializeUitls.serializeAsXml(tmp));
            obj.remove("field");
        }
        if (entityClass.equals(Button.class)) {
            tmp.put("button", obj.get("button"));
            obj.put("config", SerializeUitls.serializeAsXml(tmp));
            obj.remove("button");
        }
        StringBuilder builder = new StringBuilder("insert into ");
        String entityName = entityClass.getSimpleName();
        String tableName = (String) reflectService.getStaticFieldValue(entityClass, "TABLE_NAME");
        if(null == tableName || tableName.trim().length() == 0){
            tableName = Inflector.getInstance().tableize("EC", entityName).toUpperCase();
        }
        if (null == isEc || !isEc) {
            for (String ename : BAPNamingStrategy.entities) {
                if (entityName.equals(ename)) {
                    tableName = tableName.toUpperCase().replaceFirst("EC_", "PROJECT_");
                    break;
                }
            }
        }
        builder.append(tableName);
        StringBuilder params = new StringBuilder();
        StringBuilder fields = new StringBuilder();
//		final List<Class> types = new LinkedList<Class>();
        final List<Object> values = new LinkedList<Object>();
        final List<String> columnNames = new LinkedList<String>();
        final List<String> lobs = new LinkedList<String>();
        Object instance = entityClass.newInstance();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                try {
                    PropertyUtils.setProperty(instance, entry.getKey(), entry.getValue());
                } catch (NoSuchMethodException e) {
                    if (entityClass.equals(com.supcon.supfusion.configuration.services.entity.Field.class)) {
                        log.warn(e.getMessage());
                    }
                }
            }
        }
        List<Field> fieldList = ReflectUtils.getDeepDeclaredFields(entityClass);
        if(null != fieldList){
            for(Field field : fieldList){
                String fieldName = field.getName();
                if(!Modifier.isStatic(field.getModifiers()) && !obj.containsKey(fieldName)){
                    Transient transient1 = field.getAnnotation(Transient.class);
                    if(null != transient1){
                        continue;
                    }
                    Method method = null;
                    try {
                        method = reflectService.getMethod(entityClass, "get" +Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
                    } catch (NoSuchMethodException e) {
                        if(field.getType() == boolean.class){
                            try {
                                method = reflectService.getMethod(entityClass, "is" +Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
                            } catch (Exception e2) {
                            }
                        }
                    }
                    if(null !=method){
                        transient1 = method.getAnnotation(Transient.class);
                        if(null != transient1){
                            continue;
                        }
                        if(!ReflectUtils.isCollection(method.getReturnType())){
                            Object defaultValue = method.invoke(instance);
                            if(null != defaultValue){
                                obj.put(fieldName, defaultValue);
                            }
                        }
                    }}
            }
        }
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                String fieldName = entry.getKey();
                Field field = reflectService.getDeepField(entityClass, fieldName);
                Method method = null;
                try {
                    method = reflectService.getMethod(entityClass, "get" +Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
                } catch (NoSuchMethodException e) {
                    if(field.getType() == boolean.class){
                        try {
                            method = reflectService.getMethod(entityClass, "is" +Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
                        } catch (Exception e2) {
                            log.warn(e.getMessage(),e);
                        }
                    }
                }
                if(null != method){
                    Column column = field.getAnnotation(Column.class);
                    String columnName = null;
                    if (null != column) {
                        columnName = column.name();
                    }
                    if (null == columnName || columnName.trim().length() == 0) {
                        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                        if (null != joinColumn) {
                            columnName = joinColumn.name();
                        }
                    }
                    if (null == columnName || columnName.trim().length() == 0) {
                        columnName = Inflector.getInstance().columnize(fieldName).toUpperCase();
                    }
                    Lob lob = field.getAnnotation(Lob.class);
                    if (null != lob) {
                        lobs.add(columnName);
                    }
                    if(columnName.equals("FUNCTION_ES5")){
                        columnName="EVENT_FUNCTION_ES5";
                    }
                    if(columnName.equals("FUNCTION")){
                        columnName="EVENT_FUNCTION";
                    }
                    columnNames.add(columnName);
                    fields.append("," + columnName);
                    params.append(",?");
                    values.add(entry.getValue());
                }
            }
        }
        if (fields.length() > 0) {
            if(instance instanceof AbstractEcAuditEntity){
                columnNames.add("CREATE_TIME");
                fields.append(",CREATE_TIME");
                params.append(",?");
                values.add(new Date());
            }
            builder.append(" (").append(fields.substring(1)).append(")").append(" values (").append(params.substring(1)).append(")");
            final LobHandler lobHandler = new DefaultLobHandler();
            jdbcTemplate.execute(builder.toString(), new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
                @Override
                protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException, DataAccessException {
                    for (int i = 1; i <= columnNames.size(); i++) {
                        Object value = values.get(i - 1);
                        if (lobs.contains(columnNames.get(i - 1))) {
                            lobCreator.setClobAsCharacterStream(ps, i	, new StringReader(value.toString()), value.toString().length());
                        } else if (value instanceof String) {
                            ps.setString(i, value.toString());
                        } else if (value instanceof Integer) {
                            ps.setInt(i, Integer.parseInt(value.toString()));
                        } else if (value instanceof Long) {
                            ps.setLong(i, Long.parseLong(value.toString()));
                        } else if (value instanceof Boolean) {
                            ps.setBoolean(i, Boolean.parseBoolean(value.toString()));
                        } else if (value instanceof BigDecimal) {
                            ps.setBigDecimal(i, new BigDecimal(value.toString()));
                        } else if (value instanceof Double) {
                            ps.setDouble(i, Double.parseDouble(value.toString()));
                        } else if (value instanceof Float) {
                            ps.setFloat(i, Float.parseFloat(value.toString()));
                        } else if (value instanceof Short) {
                            ps.setShort(i, Short.parseShort(value.toString()));
                        } else if (value instanceof Date) {
                            ps.setTimestamp(i, new Timestamp(new Date().getTime()));
                        } else if (value instanceof AbstractCodeEntity) {
                            Object code = null;
                            try {
                                code = reflectService.getFieldValue(value, "code");
                            } catch (Exception e) {
                            }
                            if (null != code) {
                                ps.setString(i, code.toString());
                            } else {
                                ps.setString(i, "");
                            }
                        } else if (value instanceof AbstractIdEntity) {
                            Object id = null;
                            try {
                                id = reflectService.getFieldValue(value, "id");
                            } catch (Exception e) {
                            }
                            if (null != id) {
                                ps.setLong(i, Long.parseLong(id.toString()));
                            } else {
                                ps.setString(i, null);
                            }
                        } else if (value instanceof CodeEntity) {
                            Object id = null;
                            try {
                                id = reflectService.getFieldValue(value, "id");
                            } catch (Exception e) {
                            }
                            if (null != id) {
                                ps.setString(i, id.toString());
                            } else {
                                ps.setString(i, null);
                            }
                        } else if (value instanceof Enum) {
                            ps.setString(i, value.toString());
                        }
                    }
                }

            });
        }
    }

    /**
     * 插入对象
     *
     * @param entityClass
     *            类
     * @param obj
     *            要插入的对象
     * @param isEc
     *            是否向EC表插入
     * @throws Exception
     */
    private void updateObject(Class<?> entityClass, Map<String, Object> obj, Boolean isEc) throws Exception {
        Object code = null;
        Map<String, Object> tmp = new HashMap<>();
        if (entityClass.equals(com.supcon.supfusion.configuration.services.entity.Field.class)) {
            tmp.put("field", obj.get("field"));
            obj.put("config", SerializeUitls.serializeAsXml(tmp));
            obj.remove("field");
        }
        if (entityClass.equals(Button.class)) {
            tmp.put("button", obj.get("button"));
            obj.put("config", SerializeUitls.serializeAsXml(tmp));
            obj.remove("button");
        }
        StringBuilder builder = new StringBuilder("update ");
        StringBuilder argsColumns = new StringBuilder();
        StringBuilder argsWheres = new StringBuilder();
        String entityName = entityClass.getSimpleName();
        String tableName = (String) reflectService.getStaticFieldValue(entityClass, "TABLE_NAME");
        if(null == tableName || tableName.trim().length() == 0){
            tableName = Inflector.getInstance().tableize("EC", entityName).toUpperCase();
        }
        if (null == isEc || !isEc) {
            for (String ename : BAPNamingStrategy.entities) {
                if (entityName.equals(ename)) {
                    tableName = tableName.toUpperCase().replaceFirst("EC_", "PROJECT_");
                    break;
                }
            }
        }
        builder.append(tableName + " set ");
        final List<Object> values = new LinkedList<Object>();
        final List<String> columnNames = new LinkedList<String>();
        final List<String> lobs = new LinkedList<String>();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String fieldName = entry.getKey();
            Field field = reflectService.getDeepField(entityClass, fieldName);
            Method method = null;
            try {
                method = reflectService.getMethod(entityClass, "get" +Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
            } catch (NoSuchMethodException e) {
                if(field.getType() == boolean.class){
                    try {
                        method = reflectService.getMethod(entityClass, "is" +Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
                    } catch (Exception e2) {
                        log.warn(e.getMessage(),e);
                    }
                }
            }
            if(null != method){
                Column column = field.getAnnotation(Column.class);
                String columnName = null;
                if (null != column) {
                    columnName = column.name();
                }
                if (null == columnName || columnName.trim().length() == 0) {
                    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
                    if (null != joinColumn) {
                        columnName = joinColumn.name();
                    }
                }
                if (null == columnName || columnName.trim().length() == 0) {
                    columnName = Inflector.getInstance().columnize(fieldName).toUpperCase();
                }
                Lob lob = field.getAnnotation(Lob.class);
                if (null != lob) {
                    lobs.add(columnName);
                }
                if(columnName.equals("FUNCTION_ES5")){
                    columnName="EVENT_FUNCTION_ES5";
                }
                if(columnName.equals("FUNCTION")){
                    columnName="EVENT_FUNCTION";
                }
                if (entry.getKey() != null && !"code".equals(entry.getKey())) {
                    if (entry.getValue() != null) {
                        argsColumns.append("," + columnName + "= ?");
                        columnNames.add(columnName);
                        values.add(entry.getValue());
                    } else {
                        argsColumns.append("," + columnName + "= null");
                    }
                } else if (entry.getKey() != null && "code".equals(entry.getKey())) {
                    code = entry.getValue();
                    argsWheres.append(" where ").append(columnName).append(" = '").append(code).append("'");
                }
            }
        }
        if(argsColumns.length() > 0 && argsWheres.length() > 0){
            if(ReflectUtils.isExtends(entityClass, AbstractAuditUniqueCodeEntity.class)){
                argsColumns.append(",MODIFY_TIME = ? ");
                columnNames.add("MODIFY_TIME");
                values.add(new Date());
            }
            builder.append(argsColumns.substring(1)).append(argsWheres);
            final LobHandler lobHandler = new DefaultLobHandler();
            jdbcTemplate.execute(builder.toString(), new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
                @Override
                protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException, DataAccessException {
                    for (int i = 1; i <= columnNames.size(); i++) {
                        Object value = values.get(i - 1);
                        if (lobs.contains(columnNames.get(i - 1))) {
                            lobCreator.setClobAsCharacterStream(ps, i, new StringReader(value.toString()), value.toString().length());
                        } else if (value instanceof String) {
                            ps.setString(i, value.toString());
                        } else if (value instanceof Integer) {
                            ps.setInt(i, Integer.parseInt(value.toString()));
                        } else if (value instanceof Long) {
                            ps.setLong(i, Long.parseLong(value.toString()));
                        } else if (value instanceof Boolean) {
                            ps.setBoolean(i, Boolean.parseBoolean(value.toString()));
                        } else if (value instanceof BigDecimal) {
                            ps.setBigDecimal(i, new BigDecimal(value.toString()));
                        } else if (value instanceof Double) {
                            ps.setDouble(i, Double.parseDouble(value.toString()));
                        } else if (value instanceof Float) {
                            ps.setFloat(i, Float.parseFloat(value.toString()));
                        } else if (value instanceof Short) {
                            ps.setShort(i, Short.parseShort(value.toString()));
                        } else if (value instanceof Date) {
                            ps.setTimestamp(i, new Timestamp(new Date().getTime()));
                        } else if (value instanceof AbstractCodeEntity) {
                            Object code = null;
                            try {
                                code = reflectService.getFieldValue(value, "code");
                            } catch (Exception e) {
                            }
                            if (null != code) {
                                ps.setString(i, code.toString());
                            } else {
                                ps.setString(i, "");
                            }
                        } else if (value instanceof AbstractIdEntity) {
                            Object id = null;
                            try {
                                id = reflectService.getFieldValue(value, "id");
                            } catch (Exception e) {
                            }
                            if (null != id) {
                                ps.setLong(i, Long.parseLong(id.toString()));
                            } else {
                                ps.setString(i, null);
                            }
                        } else if (value instanceof CodeEntity) {
                            Object id = null;
                            try {
                                id = reflectService.getFieldValue(value, "id");
                            } catch (Exception e) {
                            }
                            if (null != id) {
                                ps.setString(i, id.toString());
                            } else {
                                ps.setString(i, null);
                            }
                        } else if (value instanceof Enum) {
                            ps.setString(i, value.toString());
                        }
                    }
                }
            });
        }
    }

    private List<Event> getProjViewEvent(View view){
        List<Event> eventList=new ArrayList<Event>();
        ExtraView ev=view.getExtraView();
        if(ev!=null&&ev.getConfigMap()!=null){
            Map<String, Object> layout = (Map<String, Object>) view.getExtraView().getConfigMap().get("layout");
            if(layout!=null&&layout.get("layoutCode")!=null){
                String layoutCode=layout.get("layoutCode").toString();
                Event onloadev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_onload_project");
                if(onloadev!=null){
                    eventList.add(onloadev);
                }
                Event onsaveev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_onsave_project");
                if(onsaveev!=null){
                    eventList.add(onsaveev);
                }
            }
        }
        if(view.getType()== ViewType.LIST||view.getType()==ViewType.REFERENCE){
            if(ev!=null&&ev.getConfigMap()!=null){
                Map<String, Object> layout = (Map<String, Object>) ev.getConfigMap().get("layout");
                if(layout!=null&&layout.get("layoutCode")!=null){
                    String layoutCode=layout.get("layoutCode").toString();
                    if (layout != null && !layout.isEmpty()) {
                        Event initev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_ptinit_project");
                        if(initev!=null){
                            eventList.add(initev);
                        }
                        Event rendoverev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_ptrendover_project");
                        if(rendoverev!=null){
                            eventList.add(rendoverev);
                        }
                        Event dbclickev=eventService.getEvent(view.getCode()+"_"+layoutCode+"_ptdbclick_project");
                        if(dbclickev!=null){
                            eventList.add(dbclickev);
                        }
                        Event selectFirstRowev=eventService.getEvent(view.getCode()+"_selectFirstRow_project");
                        if(selectFirstRowev!=null){
                            eventList.add(selectFirstRowev);
                        }
                        Event isExportExcelev=eventService.getEvent(view.getCode()+"_isExportExcel_project");
                        if(isExportExcelev!=null){
                            eventList.add(isExportExcelev);
                        }
                        Event isFirstLoadev=eventService.getEvent(view.getCode()+"_isFirstLoad_project");
                        if(isFirstLoadev!=null){
                            eventList.add(isFirstLoadev);
                        }
                    }
                }
            }
        }else{
            List<DataGrid> dglist=dataGridService.getDataGridByView(view, false);
            for(DataGrid dg:dglist){
                if(dg.getConfigMap()!=null){
                    Event projrenderev=eventService.getEvent(dg.getCode()+"_renderOver_project");
                    if(projrenderev!=null){
                        eventList.add(projrenderev);
                    }
                    Event projinitev=eventService.getEvent(dg.getCode()+"_ptPageInit_project");
                    if(projinitev!=null){
                        eventList.add(projinitev);
                    }
                }
            }
        }
        return eventList;
    }
}
