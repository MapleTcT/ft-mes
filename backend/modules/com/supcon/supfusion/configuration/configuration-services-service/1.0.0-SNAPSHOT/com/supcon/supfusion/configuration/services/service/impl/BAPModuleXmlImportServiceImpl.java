/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.service.MsModuleService;
import com.supcon.supfusion.configuration.services.utils.*;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import com.supcon.supfusion.base.entities.Script;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.entities.Deployment;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.EcEntityEnum;
import com.supcon.supfusion.configuration.services.utils.Inflector;
import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractIdEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.*;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.Transient;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.lang.InstantiationException;

/**
 * XML实体配置Runtime和Ec导入
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Slf4j
@ServiceApiService
public class BAPModuleXmlImportServiceImpl extends BaseServiceImpl<Module> implements BAPModuleXmlImportService, InitializingBean {

	private static final String CACHE_NAME = "menus";
	private static final String RUNTIME_PROPERTIES_CACHE_NAME = "RuntimeProperties_RuntimeCache";
	private List<Map<String, String>> ecRelations = new ArrayList<Map<String, String>>();

	private Set<String> errorFieldTypeMap = new HashSet<String>();
	@SuppressWarnings("rawtypes")
	private TreeMap<String, Class> fieldTypeMap = new TreeMap<String, Class>();

	@Autowired
	private MenuInfoService menuInfoService;
	@Autowired
	private ViewService viewService;
	@Autowired
	private MenuOperateService menuOperateService;
	@Autowired
	private ScriptService scriptService;
	@Autowired
	private ProcessService processService;
	@Autowired
	private CompanyService companyService;
	@Autowired
	private BAPExtensionService bapExtensionService;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private ReflectService reflectService;
	@Autowired
	private ActionViewService actionViewService;
	@Autowired
	private EcDataSynchronizeService ecDataSynchronizeService ;
	@Autowired
	private InternationalService internationalService;
	@Autowired
	private CustomMenuInfoService customMenuInfoService;
	@Autowired
	private PropertyService propertyService;
	@Autowired
	private MsModuleService msModuleService;
	@Value("${bap.suposinit:true}")
	private Boolean ifSupos;
	@Value("${bap.company.single:false}")
	private Boolean isSingleMode = false;
	@Value("${bap.default.company.code:''}")
	private String defaultCompanyCode;
	private static final String[] ENTITIES = new String[] { "Module", "ModuleRelation", "Entity", "Model", "Property", "View", "DataGroup", "DataClassific",
			"Sql", "ExtraQueryJson", "Field", "CustomerCondition", "Button", "DataGrid", "Event", "Validate", "SpecialPermission" };
	private static final String[] ENTITIECLASSNAMES= new String[] {"Module","com.supcon.supfusion.configuration.services.entity.Model"};


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional(propagation = Propagation.REQUIRED, timeout = -1)
	public synchronized Module importXml(String xml, Boolean uploadWorkFlow, SessionFactory sessionFactory, boolean filter, String... env) {
		if (null == sessionFactory) {
			return null;
		}
		long currentMill = System.currentTimeMillis();
		//EcUtils.uploadlog.info("上载module.xml开始 at:" + currentMill);
		Session session = sessionFactory.getCurrentSession();
		String code = null;
		Module module = null;
		List<Deployment> deployments = new ArrayList<Deployment>();
		List<MenuInfo> menuInfos = new ArrayList<MenuInfo>();
		List<Script> scripts = new ArrayList<Script>();
		List<BAPExtension> extensions = new LinkedList<BAPExtension>();
		Document doc;
		Map<String, List<Map<String, Object>>> metaMap = new HashMap<>();// 元数据信息
		for (EcEntityEnum enumItem : EcEntityEnum.values()) {
			metaMap.put(enumItem.clazz.getName(), new ArrayList<Map<String, Object>>());
		}
		try {
			code = XmlUtils.getTagContent(xml, "code");
			doc = DocumentHelper.parseText(xml);
			Element moduleE = doc.getRootElement();
			module = new Module();
			module.setCode(code);
			module.setVersion(0);
			getLastMetaItemAdd(metaMap, Module.class.getName());
			for (Iterator moduleIt = moduleE.elementIterator(); moduleIt.hasNext();) {
				Element modulePropertyE = (Element) moduleIt.next();
				String modulePropertyName = modulePropertyE.getName();
				//log.info("modulePropertyName++++++++++++++++++"+modulePropertyName);
				if ("entities".equals(modulePropertyName)) {
					// 处理Entity
					for (Iterator entitiesIt = modulePropertyE.elementIterator("entity"); entitiesIt.hasNext();) {
						Element entityE = (Element) entitiesIt.next();
						Entity entity = new Entity();
						entity.setVersion(0);
						entity.setCode(XmlUtils.getTagContent(entityE.asXML(), "code"));
						String entityCode = entity.getCode();
						getLastMetaItemAdd(metaMap, Entity.class.getName()).put("module", module);

						for (Iterator entityIt = entityE.elementIterator(); entityIt.hasNext();) {
							Element entityPropertyE = (Element) entityIt.next();
							String entityPropertyName = entityPropertyE.getName();
							if ("models".equals(entityPropertyName)) {

								for (Iterator modelsIt = entityPropertyE.elementIterator("model"); modelsIt.hasNext();) {
									Element modelE = (Element) modelsIt.next();
									Model model = new Model();
									model.setCode(XmlUtils.getTagContent(modelE.asXML(), "code"));
									model.setVersion(0);
									getLastMetaItemAdd(metaMap, Model.class.getName()).put("entity", entity);
									for (Iterator modelIt = modelE.elementIterator(); modelIt.hasNext();) {
										Element modelPropertyE = (Element) modelIt.next();
										String modelPropertyName = modelPropertyE.getName();
										if ("properties".equals(modelPropertyName)) {

											for (Iterator propertiesIt = modelPropertyE.elementIterator("property"); propertiesIt.hasNext();) {
												Element propertyE = (Element) propertiesIt.next();
												getLastMetaItemAdd(metaMap, Property.class.getName()).put("model", model);
												for (Iterator propertyIt = propertyE.elementIterator(); propertyIt.hasNext();) {
													Element propertyPropertyE = (Element) propertyIt.next();
													String propertyPropertyName = propertyPropertyE.getName();
													if ("associatedProperty".equals(propertyPropertyName)) {
														// 含code
														Element associatedPropertyCodeE = propertyPropertyE.element("code");
														if (null != associatedPropertyCodeE && associatedPropertyCodeE.getTextTrim().length() > 0) {
															Property assPro = new Property();
															assPro.setCode(associatedPropertyCodeE.getTextTrim());
															assPro.setVersion(0);
															getLastMetaItem(metaMap, Property.class.getName()).put(propertyPropertyName, assPro);
														}else{
															getLastMetaItem(metaMap, Property.class.getName()).put(propertyPropertyName, null);
														}
													} else {
														// Model simple property
														String propertyPropertyValue = propertyPropertyE.getTextTrim();
														if (propertyPropertyValue.length() > 0) {
															Object o = getObjectValue(Property.class, propertyPropertyName, propertyPropertyValue);
															getLastMetaItem(metaMap, Property.class.getName()).put(propertyPropertyName, o);
														} else {
															getLastMetaItem(metaMap, Property.class.getName()).put(propertyPropertyName, null);
														}
													}
												}
												if (null == getLastMetaItem(metaMap, Property.class.getName()).get("moduleCode")) {
													getLastMetaItem(metaMap, Property.class.getName()).put("moduleCode", module.getCode());
													getLastMetaItem(metaMap, Property.class.getName()).put("entityCode", entityCode);
												}
											}
										} else if ("sqlmodel".equals(modelPropertyName)) {
											getLastMetaItemAdd(metaMap, SqlModel.class.getName());
											for (Iterator sqlmodelIt = modelPropertyE.elementIterator(); sqlmodelIt.hasNext();) {
												Element sqlmodelE = (Element) sqlmodelIt.next();
												String sqlmodelName = sqlmodelE.getName();
												String sqlmodelValue = sqlmodelE.getText();
												if (sqlmodelValue.length() > 0) {
													getLastMetaItem(metaMap, SqlModel.class.getName()).put(sqlmodelName,
															getObjectValue(SqlModel.class, sqlmodelName, sqlmodelValue));
												}else{
													getLastMetaItem(metaMap, SqlModel.class.getName()).put(sqlmodelName,null);
												}
											}
										} else {
											// Model simple property
											String modelPropertyValue = modelPropertyE.getTextTrim();
											if (modelPropertyValue.length() > 0) {
												getLastMetaItem(metaMap, Model.class.getName()).put(modelPropertyName,
														getObjectValue(Model.class, modelPropertyName, modelPropertyValue));
											}else{
												getLastMetaItem(metaMap, Model.class.getName()).put(modelPropertyName,null);
												
											}
										}
									}
									if (null == getLastMetaItem(metaMap, Model.class.getName()).get("moduleCode")) {
										getLastMetaItem(metaMap, Model.class.getName()).put("moduleCode", module.getCode());
									}
								}

							} else if ("views".equals(entityPropertyName)) {
								for (Iterator viewsIt = entityPropertyE.elementIterator("view"); viewsIt.hasNext();) {
									Element viewE = (Element) viewsIt.next();
									getLastMetaItemAdd(metaMap, View.class.getName()).put("entity", entity);
									View view = new View();
									view.setCode(XmlUtils.getTagContent(viewE.asXML(), "code"));
									view.setVersion(0);
									String viewEnv = XmlUtils.getTagContent(viewE.asXML(), "ecEnv");
									view.setEcEnv((viewEnv != null && !viewEnv.isEmpty()) ? EcEnv.valueOf(viewEnv) : PropertyHolder.getEcEnv());
									for (Iterator viewIt = viewE.elementIterator(); viewIt.hasNext();) {
										Element viewPropertyE = (Element) viewIt.next();
										String viewPropertyName = viewPropertyE.getName();
										if ("reference".equals(viewPropertyName) || "shadowView".equals(viewPropertyName)
												|| "batchControlPrintSelectView".equals(viewPropertyName)) {
											// 含code
											Element codeE = viewPropertyE.element("code");
											if (null != codeE && codeE.getTextTrim().length() > 0) {
												View v = new View();
												v.setCode(codeE.getTextTrim());
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName, v);
											}else{
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName, null);
											}
										} else if ("assModel".equals(viewPropertyName)) {
											// 含code
											Element codeE = viewPropertyE.element("code");
											if (null != codeE && codeE.getTextTrim().length() > 0) {
												Model assModel = new Model();
												assModel.setCode(codeE.getTextTrim());
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName, assModel);
											}else{
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName, null);
											}
										} else if ("assView".equals(viewPropertyName)) {
											// 含code
											Element codeE = viewPropertyE.element("code");
											getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName, null);
											if (null != codeE && codeE.getTextTrim().length() > 0) {
												View assView = new View();
												assView.setCode(codeE.getTextTrim());
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName, assView);
											}
										} else if ("extraView".equals(viewPropertyName)) {
											// 含config
											getLastMetaItemAdd(metaMap, ExtraView.class.getName()).put("view", view);
											getLastMetaItem(metaMap, ExtraView.class.getName()).put("code", view.getCode());
											getLastMetaItem(metaMap, ExtraView.class.getName()).put("ecEnv", view.getEcEnv());
											Element codeE = viewPropertyE.element("config");
											getLastMetaItem(metaMap, ExtraView.class.getName()).put("config", null);
											getLastMetaItem(metaMap, ExtraView.class.getName()).put("fullConfig", null);
											if (null != codeE) {
												codeE = codeE.element("config");
													if (codeE != null) {
														String fullConfig = codeE.asXML();
														getLastMetaItem(metaMap, ExtraView.class.getName()).put("config", fullConfig);
														getLastMetaItem(metaMap, ExtraView.class.getName()).put("fullConfig", fullConfig);
													}
												}
												// 含viewJson
												getLastMetaItem(metaMap, ExtraView.class.getName()).put("viewJson", null);
												Element viewJsonE = viewPropertyE.element("viewJson");
												if (null != viewJsonE) {
													String viewJson = viewJsonE.getText();
													getLastMetaItem(metaMap, ExtraView.class.getName()).put("viewJson", viewJson);
												}
										} else if ("fastQueryJson".equals(viewPropertyName)) {
											getLastMetaItemAdd(metaMap, FastQueryJson.class.getName()).put("view", view);
											getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("code", view.getCode());
											getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("ecEnv", view.getEcEnv());
											// 含queryConfig
											Element codeE = viewPropertyE.element("queryConfig");
											getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("queryConfig",null);
											if (null != codeE) {
												codeE = codeE.element("config");
												if (codeE != null) {
													getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("queryConfig", codeE.asXML());
												}
											}
										} else if ("fastQueryJsons".equals(viewPropertyName)) {
											for (Iterator fastQueryJsonsIt = viewPropertyE.elementIterator("fastQueryJson"); fastQueryJsonsIt.hasNext();) {
												Element fastQueryJsonE = (Element) fastQueryJsonsIt.next();
												getLastMetaItemAdd(metaMap, FastQueryJson.class.getName()).put("view", view);
												getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("ecEnv", view.getEcEnv());

												for (Iterator fastQueryJsonIt = fastQueryJsonE.elementIterator(); fastQueryJsonIt.hasNext();) {
													Element fastQueryJsonPropertyE = (Element) fastQueryJsonIt.next();
													String fastQueryJsonPropertyName = fastQueryJsonPropertyE.getName();
													if ("queryConfig".equals(fastQueryJsonPropertyName) ) {
														Element codeE = fastQueryJsonPropertyE;
														while(null != codeE && null != codeE.element("queryConfig")){
															codeE = codeE.element("queryConfig");
														}
														if (null != codeE) {
															String fullConfig = codeE.asXML();
															getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("queryConfig", fullConfig);
														}else{
															getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("queryConfig",null);
														}
													} else if ("layoutName".equals(fastQueryJsonPropertyName)) {
														Element layoutNameE = fastQueryJsonPropertyE;
														getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("layoutName",null);
														if (null != layoutNameE) {
															String layoutName = layoutNameE.getTextTrim();
															if (layoutName != null) {
																getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("layoutName", layoutName);
															}
														}
													} else if ("code".equals(fastQueryJsonPropertyName)) {
														Element codeE = fastQueryJsonPropertyE;
														getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("code",null);
														if (null != codeE) {
															String fqjCode = codeE.getTextTrim();
															if (fqjCode != null) {
																getLastMetaItem(metaMap, FastQueryJson.class.getName()).put("code", fqjCode);
															}
														}
													} else if ("targetModel".equals(fastQueryJsonPropertyName)){
														Element codeE = fastQueryJsonPropertyE.element("code");
														if (null != codeE && codeE.getTextTrim().length() > 0) {
															Model m = new Model();
															m.setCode(codeE.getTextTrim());
															getLastMetaItem(metaMap, FastQueryJson.class.getName()).put(fastQueryJsonPropertyName, m);
														}else{
															getLastMetaItem(metaMap, FastQueryJson.class.getName()).put(fastQueryJsonPropertyName, null);
														}
													}
												}
											}
										} else if ("advQueryJsons".equals(viewPropertyName)) {
											for (Iterator advQueryJsonsIt = viewPropertyE.elementIterator("advQueryJson"); advQueryJsonsIt.hasNext();) {
												Element advQueryJsonE = (Element) advQueryJsonsIt.next();
												getLastMetaItemAdd(metaMap, AdvQueryJson.class.getName()).put("view", view);
												getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("ecEnv", view.getEcEnv());

												for (Iterator advQueryJsonIt = advQueryJsonE.elementIterator(); advQueryJsonIt.hasNext();) {
													Element advQueryJsonPropertyE = (Element) advQueryJsonIt.next();
													String advQueryJsonPropertyName = advQueryJsonPropertyE.getName();
													if ("queryConfig".equals(advQueryJsonPropertyName) ) {
														Element codeE = advQueryJsonPropertyE;
														while(null != codeE && null != codeE.element("queryConfig")){
															codeE = codeE.element("queryConfig");
														}
														if (null != codeE) {
															String fullConfig = codeE.asXML();
															getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("queryConfig", fullConfig);
														}else{
															getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("queryConfig", null);
														}
													} else if ("layoutName".equals(advQueryJsonPropertyName) ) {
														Element layoutNameE = advQueryJsonPropertyE;
														getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("layoutName", null);
														if (null != layoutNameE) {
															String layoutName = layoutNameE.getTextTrim();
															if (layoutName != null) {
																getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("layoutName", layoutName);
															}
														}
													} else if ("name".equals(advQueryJsonPropertyName) ) {
														Element nameE = advQueryJsonPropertyE;
														getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("name",null);
														if (null != nameE) {
															String name = nameE.getTextTrim();
															if (name != null) {
																getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("name", name);
															}
														}
													} else if ("code".equals(advQueryJsonPropertyName)) {
														Element codeE = advQueryJsonPropertyE;
														getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("code",null);
														if (null != codeE) {
															String aqjCode = codeE.getTextTrim();
															if (aqjCode != null) {
																getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put("code", aqjCode);
															}
														}
													} else if ("targetModel".equals(advQueryJsonPropertyName)){
														Element codeE = advQueryJsonPropertyE.element("code");
														if (null != codeE && codeE.getTextTrim().length() > 0) {
															Model m = new Model();
															m.setCode(codeE.getTextTrim());
															getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put(advQueryJsonPropertyName, m);
														}else{
															getLastMetaItem(metaMap, AdvQueryJson.class.getName()).put(advQueryJsonPropertyName, null);
														}
													}
												}
											}
										} else if ("defaultAdvCond".equals(viewPropertyName)) {
											getLastMetaItemAdd(metaMap, DefaultAdvCond.class.getName()).put("viewCode", view.getCode());
											getLastMetaItem(metaMap, DefaultAdvCond.class.getName()).put("ecEnv", view.getEcEnv());
											// 含content
											for (Iterator defaultAdvCondIt = viewPropertyE.elementIterator(); defaultAdvCondIt.hasNext();) {
												Element defaultAdvCondPropertyE = (Element) defaultAdvCondIt.next();
												String defaultAdvCondPropertyName = defaultAdvCondPropertyE.getName();
												String defaultAdvCondPropertyValue = defaultAdvCondPropertyE.getTextTrim();

												if (defaultAdvCondPropertyValue.length() > 0) {
													getLastMetaItem(metaMap, DefaultAdvCond.class.getName()).put(defaultAdvCondPropertyName,
															getObjectValue(DefaultAdvCond.class, defaultAdvCondPropertyName, defaultAdvCondPropertyValue));
												}else{
													getLastMetaItem(metaMap, DefaultAdvCond.class.getName()).put(defaultAdvCondPropertyName,null);
												}
											}
										} else if ("extraQueryJson".equals(viewPropertyName)) {
											getLastMetaItemAdd(metaMap, ExtraQueryJson.class.getName()).put("view", view);
											getLastMetaItem(metaMap, ExtraQueryJson.class.getName()).put("code", view.getCode());
											getLastMetaItem(metaMap, ExtraQueryJson.class.getName()).put("ecEnv", view.getEcEnv());
											// 含queryConfig
											Element codeE = viewPropertyE.element("queryConfig");
											if (null != codeE && codeE.getTextTrim().length() > 0) {
												getLastMetaItem(metaMap, ExtraQueryJson.class.getName()).put("queryConfig", codeE.getTextTrim());
											}else{
												getLastMetaItem(metaMap, ExtraQueryJson.class.getName()).put("queryConfig",null);
											}
										} else if ("sqls".equals(viewPropertyName)) {
											for (Iterator sqlsIt = viewPropertyE.elementIterator("sql"); sqlsIt.hasNext();) {
												Element sqlE = (Element) sqlsIt.next();
												getLastMetaItemAdd(metaMap, Sql.class.getName());
												for (Iterator sqlIt = sqlE.elementIterator(); sqlIt.hasNext();) {
													Element sqlPropertyE = (Element) sqlIt.next();
													String sqlPropertyName = sqlPropertyE.getName();
													String sqlPropertyValue = sqlPropertyE.getTextTrim();
													if (sqlPropertyValue.length() > 0) {
														getLastMetaItem(metaMap, Sql.class.getName()).put(sqlPropertyName,
																getObjectValue(Sql.class, sqlPropertyName, sqlPropertyValue));
													}else{
														getLastMetaItem(metaMap, Sql.class.getName()).put(sqlPropertyName,null);
													}
												}
											}
										} else if ("dataGrids".equals(viewPropertyName)) {
											for (Iterator dataGridsIt = viewPropertyE.elementIterator("dataGrid"); dataGridsIt.hasNext();) {
												// dataGrid.setView(view);
												Element dataGridE = (Element) dataGridsIt.next();
												getLastMetaItemAdd(metaMap, DataGrid.class.getName()).put("view", view);
												code = XmlUtils.getTagContent(dataGridE.asXML(), "code");// datagrid
																											// code
												for (Iterator dataGridIt = dataGridE.elementIterator(); dataGridIt.hasNext();) {
													Element dataGridPropertyE = (Element) dataGridIt.next();
													String dataGridPropertyName = dataGridPropertyE.getName();
													if ("targetModel".equals(dataGridPropertyName)) {
														Element codeE = dataGridPropertyE.element("code");
														if (null != codeE && codeE.getTextTrim().length() > 0) {
															Model m = new Model();
															m.setCode(codeE.getTextTrim());
															getLastMetaItem(metaMap, DataGrid.class.getName()).put(dataGridPropertyName, m);
														}else{
															getLastMetaItem(metaMap, DataGrid.class.getName()).put(dataGridPropertyName, null);
														}
													} else if ("orgProperty".equals(dataGridPropertyName)) {
														Element codeE = dataGridPropertyE.element("code");
														if (null != codeE && codeE.getTextTrim().length() > 0) {
															Property property = new Property();
															property.setCode(codeE.getTextTrim());
															property.setVersion(0);
															getLastMetaItem(metaMap, DataGrid.class.getName()).put(dataGridPropertyName, property);
														}else{
															getLastMetaItem(metaMap, DataGrid.class.getName()).put(dataGridPropertyName, null);
														}
													} else if ("config".equals(dataGridPropertyName) && null != dataGridPropertyE) {
														Element codeE = dataGridPropertyE;
														if (null != codeE) {
															codeE = codeE.element("config");
															if (codeE != null) {
																String fullConfig = codeE.asXML();
																/*
																 * Map<String, Object> configMap =
																 * EcExtraViewIntegrationUtils
																 * .ecSplitConfig(fullConfig);
																 * if (configMap.get("fieldConfig") != null) {
																 * String fieldConfig =
																 * configMap.get("fieldConfig").toString();
																 * fieldsConfigMap.put(code, fieldConfig);
																 * }
																 */
																getLastMetaItem(metaMap, DataGrid.class.getName()).put("config", fullConfig);
																getLastMetaItem(metaMap, DataGrid.class.getName()).put("fullConfig", fullConfig);
															}else{
																getLastMetaItem(metaMap, DataGrid.class.getName()).put("config", null);
																getLastMetaItem(metaMap, DataGrid.class.getName()).put("fullConfig", null);
															}
														}
													} else {
														String dataGridPropertyValue = dataGridPropertyE.getTextTrim();
														if (dataGridPropertyValue.length() > 0) {
															getLastMetaItem(metaMap, DataGrid.class.getName()).put(dataGridPropertyName,
																	getObjectValue(DataGrid.class, dataGridPropertyName, dataGridPropertyValue));
														}else{
															getLastMetaItem(metaMap, DataGrid.class.getName()).put(dataGridPropertyName,null);
														}
													}
												}
												if (null == getLastMetaItem(metaMap, DataGrid.class.getName()).get("moduleCode")) {
													getLastMetaItem(metaMap, DataGrid.class.getName()).put("moduleCode", module.getCode());
													getLastMetaItem(metaMap, DataGrid.class.getName()).put("entityCode", entityCode);
												}
											}
										} else if ("dataGroups".equals(viewPropertyName)) {
											for (Iterator dataGroupsIt = viewPropertyE.elementIterator("dataGroup"); dataGroupsIt.hasNext();) {
												Element dataGroupE = (Element) dataGroupsIt.next();
												getLastMetaItemAdd(metaMap, DataGroup.class.getName()).put("view", view);
												DataGroup dataGroup = new DataGroup();
												dataGroup.setCode(XmlUtils.getTagContent(dataGroupE.asXML(), "code"));
												dataGroup.setVersion(0);
												for (Iterator dataGroupIt = dataGroupE.elementIterator(); dataGroupIt.hasNext();) {
													Element dataGroupPropertyE = (Element) dataGroupIt.next();
													String dataGroupPropertyName = dataGroupPropertyE.getName();
													if ("dataClassifics".equals(dataGroupPropertyName)) {
														for (Iterator dataClassificsIt = dataGroupPropertyE.elementIterator("dataClassific"); dataClassificsIt
																.hasNext();) {
															Element dataClassificE = (Element) dataClassificsIt.next();
															getLastMetaItemAdd(metaMap, DataClassific.class.getName()).put("dataGroup", dataGroup);
															for (Iterator dataClassificIt = dataClassificE.elementIterator(); dataClassificIt.hasNext();) {
																Element dataClassificPropertyE = (Element) dataClassificIt.next();
																String dataClassificPropertyName = dataClassificPropertyE.getName();
																String dataClassificPropertyValue = dataClassificPropertyE.getTextTrim();
																if (dataClassificPropertyValue.length() > 0) {
																	getLastMetaItem(metaMap, DataClassific.class.getName()).put(
																			dataClassificPropertyName,
																			getObjectValue(DataClassific.class, dataClassificPropertyName,
																					dataClassificPropertyValue));
																}else{
																	getLastMetaItem(metaMap, DataClassific.class.getName()).put(
																			dataClassificPropertyName,null);
																}
															}
															if (null == getLastMetaItem(metaMap, DataClassific.class.getName()).get("moduleCode")) {
																getLastMetaItem(metaMap, DataClassific.class.getName()).put("moduleCode", module.getCode());
																getLastMetaItem(metaMap, DataClassific.class.getName()).put("entityCode", entityCode);
															}
														}
													}  else if ("targetModel".equals(dataGroupPropertyName)){
														Element codeE = dataGroupPropertyE.element("code");
														if (null != codeE && codeE.getTextTrim().length() > 0) {
															Model m = new Model();
															m.setCode(codeE.getTextTrim());
															getLastMetaItem(metaMap, DataGroup.class.getName()).put(dataGroupPropertyName, m);
														}else{
															getLastMetaItem(metaMap, DataGroup.class.getName()).put(dataGroupPropertyName, null);
														}
													} else {
														String dataGroupPropertyValue = dataGroupPropertyE.getTextTrim();
														if (dataGroupPropertyValue.length() > 0) {
															getLastMetaItem(metaMap, DataGroup.class.getName()).put(dataGroupPropertyName,
																	getObjectValue(DataGroup.class, dataGroupPropertyName, dataGroupPropertyValue));
														}else{
															getLastMetaItem(metaMap, DataGroup.class.getName()).put(dataGroupPropertyName,null);
														}
													}
												}
												if (null == getLastMetaItem(metaMap, DataGroup.class.getName()).get("moduleCode")) {
													getLastMetaItem(metaMap, DataGroup.class.getName()).put("moduleCode", module.getCode());
													getLastMetaItem(metaMap, DataGroup.class.getName()).put("entityCode", entityCode);
												}
											}
										} else if ("echartsList".equals(viewPropertyName)) { // 图表
											for (Iterator echartsListIt = viewPropertyE.elementIterator("echarts"); echartsListIt.hasNext();) {
												Element echartsE = (Element) echartsListIt.next();
												getLastMetaItemAdd(metaMap, Echarts.class.getName());
												for (Iterator echartsIt = echartsE.elementIterator(); echartsIt.hasNext();) {
													Element echartsPropertyE = (Element) echartsIt.next();
													String echartsPropertyName = echartsPropertyE.getName();
													String echartsPropertyValue = echartsPropertyE.getTextTrim();
													if (echartsPropertyValue.length() > 0) {
														getLastMetaItem(metaMap, Echarts.class.getName()).put(echartsPropertyName,
																getObjectValue(Echarts.class, echartsPropertyName, echartsPropertyValue));
													}else{
														getLastMetaItem(metaMap, Echarts.class.getName()).put(echartsPropertyName,null);
													}
												}
											}
										} else if ("echartsModelList".equals(viewPropertyName)) { // 图表数据源
											for (Iterator emodelListIt = viewPropertyE.elementIterator("echartsModel"); emodelListIt.hasNext();) {
												Element echartsModelE = (Element) emodelListIt.next();
												getLastMetaItemAdd(metaMap, EchartsModel.class.getName());
												for (Iterator emodelIt = echartsModelE.elementIterator(); emodelIt.hasNext();) {
													Element emodelPropertyE = (Element) emodelIt.next();
													String emodelPropertyName = emodelPropertyE.getName();
													String emodelPropertyValue = emodelPropertyE.getTextTrim();
													if (emodelPropertyValue.length() > 0) {
														getLastMetaItem(metaMap, EchartsModel.class.getName()).put(emodelPropertyName,
																getObjectValue(EchartsModel.class, emodelPropertyName, emodelPropertyValue));
													}else{
														getLastMetaItem(metaMap, EchartsModel.class.getName()).put(emodelPropertyName,null);
													}
												}
											}
										} else if ("echartsEventList".equals(viewPropertyName)) { // 图表事件
											List<Map<String, Object>> eventsList = metaMap.get(Event.class.getName());
											if (eventsList == null) {
												eventsList = new LinkedList<Map<String, Object>>();
											}
											for (Iterator eventListIt = viewPropertyE.elementIterator("event"); eventListIt.hasNext();) {
												Element eventE = (Element) eventListIt.next();
												Map<String, Object> event = new HashMap<String, Object>();
												for (Iterator eventIt = eventE.elementIterator(); eventIt.hasNext();) {
													Element eventPropertyE = (Element) eventIt.next();
													String eventPropertyName = eventPropertyE.getName();
													String eventPropertyValue = eventPropertyE.getTextTrim();
													if (eventPropertyValue.length() > 0) {
														event.put(eventPropertyName, getObjectValue(Event.class, eventPropertyName, eventPropertyValue));
													}else{
														event.put(eventPropertyName, null);
													}
												}
												eventsList.add(event);
											}
											metaMap.put(Event.class.getName(), eventsList);
										} else {
											// View simple property
											String viewPropertyValue = viewPropertyE.getTextTrim();
											if (viewPropertyValue.length() > 0) {
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName,
														getObjectValue(View.class, viewPropertyName, viewPropertyValue));
											}else{
												getLastMetaItem(metaMap, View.class.getName()).put(viewPropertyName,null);
											}
										}
									}
									if (null == getLastMetaItem(metaMap, View.class.getName()).get("moduleCode")) {
										getLastMetaItem(metaMap, View.class.getName()).put("moduleCode", module.getCode());
									}
								}

							} else if ("deployments".equals(entityPropertyName)) {
								if (uploadWorkFlow || null == env || env.length == 0 || !env[0].equals("ec")) {
									for (Iterator deploymentsIt = entityPropertyE.elementIterator("deployment"); deploymentsIt.hasNext();) {
										Element deploymentE = (Element) deploymentsIt.next();
										// <processKey>${(deployment.processKey)!}</processKey>
										// <processVersion>${(deployment.processVersion)!}</processVersion>
										Deployment deployment = null;
										if (deployment == null) {
											deployment = new Deployment();
											deployment.setVersion(0);
										}
										for (Iterator deploymentIt = deploymentE.elementIterator(); deploymentIt.hasNext();) {
											Element deploymentPropertyE = (Element) deploymentIt.next();
											String deploymentPropertyName = deploymentPropertyE.getName();
											String deploymentPropertyValue = deploymentPropertyE.getTextTrim();
											if (deploymentPropertyValue.length() > 0){
												Class type = PropertyUtils.getPropertyType(deployment, deploymentPropertyName);
												if (type != null) {
													PropertyUtils.setProperty(deployment, deploymentPropertyName,
															getObjectValue(type, deploymentPropertyValue));
												}
											}else{
												PropertyUtils.setProperty(deployment, deploymentPropertyName,null);
											}
										}
										deployment.setPublishFlag(false);
										deployment.setTempProcessXml(deployment.getProcessXml());
										deployment.setCreateTime(new Date());
										deployments.add(deployment);
									}
								}
							}  else if ("menuInfos".equals(entityPropertyName)) {
								this.dealMenuInfos(entityPropertyE, menuInfos);
							}else {
								// entityType 系统编码类型
								if ("entityType".equals(entityPropertyName)) {
									Element entityTypeIdE = entityPropertyE.element("id");
									if (null != entityTypeIdE && entityTypeIdE.getTextTrim().length() > 0) {
										SystemCode scode = new SystemCode();
										scode.setId(entityTypeIdE.getTextTrim());
										getLastMetaItem(metaMap, Entity.class.getName()).put(entityPropertyName, scode);
									}else{
										getLastMetaItem(metaMap, Entity.class.getName()).put(entityPropertyName, null);
									}
								}
								// Entity简单属性
								String entityPropertyValue = entityPropertyE.getTextTrim();
								if (entityPropertyValue.length() > 0) {
									getLastMetaItem(metaMap, Entity.class.getName()).put(entityPropertyName,
											getObjectValue(Entity.class, entityPropertyName, entityPropertyValue));
								}else{
									getLastMetaItem(metaMap, Entity.class.getName()).put(entityPropertyName,null);
								}
							}
						}
					}
				} else if ("scripts".equals(modulePropertyName)) {
					for (Iterator scriptsIt = modulePropertyE.elementIterator("script"); scriptsIt.hasNext();) {
						Script script = null;
						Element scriptE = (Element) scriptsIt.next();
						String scriptCode = XmlUtils.getTagContent(scriptE.asXML(), "scriptCode");
						String entityCode = XmlUtils.getTagContent(scriptE.asXML(), "entityCode");
						script = scriptService.get(entityCode, scriptCode);
						if (null == script) {
							script = new Script();
							script.setVersion(0);
						}
						for (Iterator scriptIt = scriptE.elementIterator(); scriptIt.hasNext();) {
							Element scriptPropertyE = (Element) scriptIt.next();
							String scriptPropertyName = scriptPropertyE.getName();
							String scriptPropertyValue = scriptPropertyE.getTextTrim();
							if (scriptPropertyValue != null && scriptPropertyValue.length() > 0) {
								Class type = PropertyUtils.getPropertyType(script, scriptPropertyName);
								if (type != null) {
									PropertyUtils.setProperty(script, scriptPropertyName, getObjectValue(type, scriptPropertyValue));
								}
							}else{
								PropertyUtils.setProperty(script, scriptPropertyName, null);
							}
						}
						scripts.add(script);
					}
				} else if ("customerConditions".equals(modulePropertyName)) {
					for (Iterator conditionsIt = modulePropertyE.elementIterator("customerCondition"); conditionsIt.hasNext();) {
						Element conditionE = (Element) conditionsIt.next();
						getLastMetaItemAdd(metaMap, CustomerCondition.class.getName());
						for (Iterator conditionIt = conditionE.elementIterator(); conditionIt.hasNext();) {
							Element conditionPropertyE = (Element) conditionIt.next();
							String conditionPropertyName = conditionPropertyE.getName();
							if (conditionPropertyName.equals("view")) {
								Element codeE = conditionPropertyE.element("code");
								if (null != codeE && codeE.getTextTrim().length() > 0) {
									View view = new View();
									view.setCode(codeE.getTextTrim());
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put("view", view);
								}else{
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put("view", null);
								}
							} else if (conditionPropertyName.equals("dataGrid")) {
								Element codeE = conditionPropertyE.element("code");
								if (null != codeE && codeE.getTextTrim().length() > 0) {
									DataGrid dataGrid = new DataGrid();
									dataGrid.setCode(codeE.getTextTrim());
									dataGrid.setVersion(0);
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put("dataGrid", dataGrid);
								}else{
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put("dataGrid", null);
								}
							} else if (conditionPropertyName.equals("dataClassific")) {
								Element codeE = conditionPropertyE.element("code");
								if (null != codeE && codeE.getTextTrim().length() > 0) {
									DataClassific classific = new DataClassific();
									classific.setCode(codeE.getTextTrim());
									classific.setVersion(0);
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put("dataClassific", classific);
								}else{
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put("dataClassific", null);
								}
							} else {
								String conditionPropertyValue = conditionPropertyE.getText();
								if (conditionPropertyValue.length() > 0) {
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put(conditionPropertyName,
											getObjectValue(CustomerCondition.class, conditionPropertyName, conditionPropertyValue));
								}else{
									getLastMetaItem(metaMap, CustomerCondition.class.getName()).put(conditionPropertyName,null);
								}
							}
						}
					}
				} else if ("specialPermissions".equals(modulePropertyName)) {
					// 导出特殊权限配置
					for (Iterator conditionsIt = modulePropertyE.elementIterator("specialPermission"); conditionsIt.hasNext();) {
						Element conditionE = (Element) conditionsIt.next();
						getLastMetaItemAdd(metaMap, SpecialPermission.class.getName());
						for (Iterator conditionIt = conditionE.elementIterator(); conditionIt.hasNext();) {
							Element conditionPropertyE = (Element) conditionIt.next();
							String conditionPropertyName = conditionPropertyE.getName();
							if (conditionPropertyName.equals("refView")) {
								Element codeE = conditionPropertyE.element("code");
								if (null != codeE && codeE.getTextTrim().length() > 0) {
									View view = new View();
									view.setCode(codeE.getTextTrim());
									getLastMetaItem(metaMap, SpecialPermission.class.getName()).put("refView", view);
								}else{
									getLastMetaItem(metaMap, SpecialPermission.class.getName()).put("refView", null);
								}
							} else if (conditionPropertyName.equals("property")) {
								Element codeE = conditionPropertyE.element("code");
								if (null != codeE && codeE.getTextTrim().length() > 0) {
									Property pro = new Property();
									pro.setCode(codeE.getTextTrim());
									pro.setVersion(0);
									getLastMetaItem(metaMap, SpecialPermission.class.getName()).put("property", pro);
								}else{
									getLastMetaItem(metaMap, SpecialPermission.class.getName()).put("property", null);
								}
							} else {
								String conditionPropertyValue = conditionPropertyE.getTextTrim();
								if (conditionPropertyValue.length() > 0) {
									getLastMetaItem(metaMap, SpecialPermission.class.getName()).put(conditionPropertyName,
											getObjectValue(SpecialPermission.class, conditionPropertyName, conditionPropertyValue));
								}else{
									getLastMetaItem(metaMap, SpecialPermission.class.getName()).put(conditionPropertyName,null);
								}
							}
						}
					}
				} else if ("relations".equals(modulePropertyName) || "references".equals(modulePropertyName)) {
				} else if ("extensions".equals(modulePropertyName)) {
					for (Iterator extensionsIt = modulePropertyE.elementIterator("bapExtension"); extensionsIt.hasNext();) {
						BAPExtension bapExtension = null;
						Element extensionE = (Element) extensionsIt.next();
						String extensionCode = XmlUtils.getTagContent(extensionE.asXML(), "code");
						bapExtension = bapExtensionService.getExtension(extensionCode);
						if (null == bapExtension) {
							bapExtension = new BAPExtension();
						}
						for (Iterator extensionIt = extensionE.elementIterator(); extensionIt.hasNext();) {
							Element extensionPropertyE = (Element) extensionIt.next();
							String extensionPropertyName = extensionPropertyE.getName();
							if ("zoneType".equals(extensionPropertyName)) {
								Element zoneTypeIdE = extensionPropertyE.element("id");
								if (null != zoneTypeIdE && zoneTypeIdE.getTextTrim().length() > 0) {
									SystemCode zoneType = new SystemCode();
									zoneType.setId(zoneTypeIdE.getTextTrim());
									bapExtension.setZoneType(zoneType);
								}
							} else {
								String extensionPropertyValue = extensionPropertyE.getTextTrim();
								if (null == extensionPropertyValue || extensionPropertyValue.length() == 0) {
									continue;
								}
								Class type = PropertyUtils.getPropertyType(bapExtension, extensionPropertyName);
								if (type != null) {
									PropertyUtils.setProperty(bapExtension, extensionPropertyName,
											getObjectValue(type, extensionPropertyValue));
								}
							}
						}
						bapExtension.setValid(true);
						extensions.add(bapExtension);
					}
				} else if ("menuInfos".equals(modulePropertyName)) {
					this.dealMenuInfos(modulePropertyE, menuInfos);
				} else {
					// Module简单属性
					String modulePropertyValue = modulePropertyE.getTextTrim();
					if (modulePropertyValue.length() > 0) {
						try{getLastMetaItem(metaMap, Module.class.getName()).put(modulePropertyName,
								getObjectValue(Module.class, modulePropertyName, modulePropertyValue));
						} catch (Exception e){
							log.error("Could not locate field '" + modulePropertyName + "' on class " + Module.class);;
						};

					}else{
						getLastMetaItem(metaMap, Module.class.getName()).put(modulePropertyName,null);
					}
					if(null != modulePropertyName && "type".equals(modulePropertyName)){
						module.setType(modulePropertyValue);
					}
				}
			}

			// 处理customerConditions
			List<Map<String, Object>> viewList = metaMap.get(View.class.getName());

			List<Map<String, Object>> customerConditionList = metaMap.get(CustomerCondition.class.getName());
			for (Map<String, Object> mapItem : customerConditionList) {
				if (null == mapItem.get("moduleCode")) {
					if (null != mapItem.get("view")) {
						View view = (View) mapItem.get("view");
						for (Map<String, Object> viewMap : viewList) {
							if (null != viewMap.get("code") && viewMap.get("code").toString().equals(view.getCode())) {
								if (null != viewMap.get("entity")) {
									Entity entity = (Entity) viewMap.get("entity");
									mapItem.put("moduleCode", module.getCode());
									mapItem.put("entityCode", entity.getCode());
								}
								break;
							}
						}
					}
				}
			}

			Set<String> uploadViewCodesForSpe = new HashSet<>();
			for (Map<String, Object> item : viewList) {
				uploadViewCodesForSpe.add((String) item.get("code"));
			}
			List<Map<String, Object>> specialPermissionList = metaMap.get(SpecialPermission.class.getName());
			for (Map<String, Object> mapItem : specialPermissionList) {
				if (null != mapItem.get("refView")) {
					View refView = (View) mapItem.get("refView");
					if (refView != null && !refView.getCode().isEmpty()) {
						if (!uploadViewCodesForSpe.contains(refView.getCode()) && viewService.getView(refView.getCode()) == null) {
							throw new EcException(EcException.Code.REFERVIEW_NOT_FOUND, refView.getCode());
						}
					}
				}
			}

			// 处理页面field、event、validate
			// 处理extraView与datagrid中的field
			List<Map<String, Object>> eventsList = new LinkedList<Map<String, Object>>();
			List<Map<String, Object>> buttonsList = new LinkedList<Map<String, Object>>();
			List<Map<String, Object>> validateList = new LinkedList<Map<String, Object>>();
			List<Map<String, Object>> fieldsList = new LinkedList<Map<String, Object>>();
			List<Map<String, Object>> values = metaMap.get(ExtraView.class.getName());
			EcExtraViewIntegrationUtilsForUpload utils = new EcExtraViewIntegrationUtilsForUpload();
			//view-->field
			for (Map<String, Object> mapItem : values) {
				String viewCode = (String) mapItem.get("code");
				String entityCode = "";
				EcEnv ecEnv = null;
				for (Map<String, Object> viewMap : viewList) {
					if (null != viewMap.get("code") && viewMap.get("code").toString().equals(viewCode)) {
						if (null != viewMap.get("entity")) {
							Entity entity = (Entity) viewMap.get("entity");
							entityCode = entity.getCode();
						}
						if (viewMap.containsKey("ecEnv") && viewMap.get("ecEnv") != null && !"".equals(viewMap.get("ecEnv"))) {
							ecEnv = EcEnv.valueOf(viewMap.get("ecEnv").toString());
						}
						break;
					}
				}

				if (ecEnv == null) {
					ecEnv = PropertyHolder.getEcEnv();
				}
				utils.ecSplitConfig(viewCode, (String) mapItem.get("config"), module.getCode(), entityCode, ecEnv, filter);
				eventsList.addAll(utils.getEventsList());
				buttonsList.addAll(utils.getButtonsList());
				validateList.addAll(utils.getValidateList());
				fieldsList.addAll(utils.getFieldsList());

			}
			//datagrid-->field
			values = metaMap.get(DataGrid.class.getName());
			for (Map<String, Object> mapItem : values) {
				String dgCode = (String) mapItem.get("code");
				String entityCode = (String) mapItem.get("entityCode");
				EcEnv ecEnv = null;
				if (mapItem.containsKey("ecEnv") && mapItem.get("ecEnv") != null && !"".equals(mapItem.get("ecEnv"))) {
					ecEnv = EcEnv.valueOf(mapItem.get("ecEnv").toString());
				} else {
					ecEnv = PropertyHolder.getEcEnv();
				}
				utils.ecSplitConfig(dgCode, (String) mapItem.get("config"), module.getCode(), entityCode, ecEnv, filter);
				eventsList.addAll(utils.getEventsList());
				buttonsList.addAll(utils.getButtonsList());
				validateList.addAll(utils.getValidateList());
				fieldsList.addAll(utils.getFieldsList());
			}

			//fastQuery-->field
			values = metaMap.get(FastQueryJson.class.getName());
			for (Map<String, Object> mapItem : values) {
				if (null == mapItem.get("layoutName") || "".equals((String) mapItem.get("layoutName"))) {
					continue;
				}
				String fqjCode = (String) mapItem.get("code");
				String entityCode = (String) mapItem.get("entityCode");
				EcEnv ecEnv = null;
				if (mapItem.containsKey("ecEnv") && mapItem.get("ecEnv") != null && !"".equals(mapItem.get("ecEnv"))) {
					ecEnv = EcEnv.valueOf(mapItem.get("ecEnv").toString());
				} else {
					ecEnv = PropertyHolder.getEcEnv();
				}
				utils.ecSplitConfig(fqjCode, (String) mapItem.get("queryConfig"), module.getCode(), entityCode, ecEnv, filter);
				eventsList.addAll(utils.getEventsList());
				buttonsList.addAll(utils.getButtonsList());
				validateList.addAll(utils.getValidateList());
				fieldsList.addAll(utils.getFieldsList());
			}
			//AdvQuery-->field
			values = metaMap.get(AdvQueryJson.class.getName());
			for (Map<String, Object> mapItem : values) {
				if (null == mapItem.get("layoutName") || "".equals((String) mapItem.get("layoutName"))) {
					continue;
				}
				String aqjCode = (String) mapItem.get("code");
				String entityCode = (String) mapItem.get("entityCode");
				EcEnv ecEnv = null;
				if (mapItem.containsKey("ecEnv") && mapItem.get("ecEnv") != null && !"".equals(mapItem.get("ecEnv"))) {
					ecEnv = EcEnv.valueOf(mapItem.get("ecEnv").toString());
				} else {
					ecEnv = PropertyHolder.getEcEnv();
				}
				utils.ecSplitConfig(aqjCode, (String) mapItem.get("queryConfig"), module.getCode(), entityCode, ecEnv, filter);
				eventsList.addAll(utils.getEventsList());
				buttonsList.addAll(utils.getButtonsList());
				validateList.addAll(utils.getValidateList());
				fieldsList.addAll(utils.getFieldsList());
			}

			// 处理编码
			List<Map<String, Object>> propertyMaps = metaMap.get(Property.class.getName());
			for (Map<String, Object> mapItem : propertyMaps) {
				if (null == mapItem.get("attributes") || "".equals((String) mapItem.get("attributes"))) {
					continue;
				}
				Property property = new Property();
				property.setCode(mapItem.get("code").toString());
				property.setAttributes(mapItem.get("attributes").toString());
				try {
					propertyService.addCounterRule(property);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			//
			LinkedList<Map<String, Object>> tmpList = new LinkedList<>();
			Map<String, Object> tmpMap = null;
			for (Map<String, Object> listItem : fieldsList) {
				tmpMap = new TreeMap<>();
				tmpList.add(tmpMap);
				for (Iterator<Map.Entry<String, Object>> it = listItem.entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, Object> mapItem = it.next();
					// map中有许多field没有的字段，但是需要保存到config中去，去掉这些字段
					if (checkIfPropertyExists(Field.class, mapItem.getKey())) {
						Object tmpObj = mapItem.getValue();
						if (tmpObj instanceof String) {
							try {
								tmpObj = getObjectValue(Field.class, mapItem.getKey(), (String) tmpObj);
								tmpMap.put(mapItem.getKey(), tmpObj);
							} catch (Exception e) {
								log.warn(e.getMessage(),e);
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

			tmpList = new LinkedList<>();
			for (Map<String, Object> listItem : buttonsList) {
				tmpMap = new HashMap<>();
				tmpList.add(tmpMap);
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
								log.warn(e.getMessage(),e);
							}
						} else {
							tmpMap.put(mapItem.getKey(), tmpObj);
						}
						// 过滤对象,存到button的config中，转换不了
						if (tmpObj instanceof AbstractCodeEntity) {
							it.remove();
						}
					}
				}
				tmpMap.put("button", listItem);
			}
			buttonsList = tmpList;

			for (Map<String, Object> listItem : validateList) {
				for (Map.Entry<String, Object> mapItem : listItem.entrySet()) {
					if (mapItem.getValue() instanceof String) {
						mapItem.setValue(getObjectValue(Validate.class, mapItem.getKey(), (String) mapItem.getValue()));
					}
				}
			}
			for (Map<String, Object> listItem : eventsList) {
				for (Map.Entry<String, Object> mapItem : listItem.entrySet()) {
					if (mapItem.getValue() instanceof String) {
						mapItem.setValue(getObjectValue(Event.class, mapItem.getKey(), (String) mapItem.getValue()));
					}
				}
			}


			metaMap.put(Field.class.getName(), fieldsList);
			metaMap.put(Button.class.getName(), buttonsList);
			metaMap.put(Validate.class.getName(), validateList);
			List<Map<String, Object>> events = metaMap.get(Event.class.getName());
			if (events != null && !events.isEmpty()) {
				eventsList.addAll(events);
			}
			metaMap.put(Event.class.getName(), eventsList);
			log.info("==================解析module.xml完成，耗时" + (System.currentTimeMillis() - currentMill) + " ms");
			log.info("=================开始刷新数据到EC========");
			currentMill = System.currentTimeMillis();
			synchronizeObjInfo(metaMap, module, session, env);
			log.info("==================刷新数据到EC完成，耗时" + (System.currentTimeMillis() - currentMill) + " ms");
			msModuleService.saveMsModule(xml, xml);
			log.info("==================存储服务信息完成，耗时" + (System.currentTimeMillis() - currentMill) + " ms");

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new EcException(e.getMessage(), e);
		}
		log.info("=================开始刷新菜单数据========");
		currentMill = System.currentTimeMillis();
		saveMenuInfo(module, menuInfos, env);
		// 保存脚本
		if (scripts != null && !scripts.isEmpty()) {
			for (Script script : scripts) {
				scriptService.save(script);
			}
		}
		if (uploadWorkFlow || null == env || env.length == 0 || !env[0].equals("ec")) {
			// 保存工作流
			if (deployments != null && !deployments.isEmpty()) {
				for (Deployment deployment : deployments) {
					boolean value = processService.repeat(deployment.getProcessKey(), deployment.getEntityCode());
					if(!value) {
						processService.saveDeployment(deployment, "", "", "", "", "", "");
					}else{
						log.info("==================环境中已存在key为" + deployment.getProcessKey() + "的工作流，无法保存");
						EcUtils.uploadFullLogger.info("<span style='color:red'>环境中已存在key为: " + deployment.getProcessKey() + "的工作流，无法保存" + "</span>");
						EcUtils.uploadLogger.info("<span style='color:red'>环境中已存在key为: " + deployment.getProcessKey() + "的工作流，无法保存" + "</span>");
					}
				}
			}
		}
		/**
		 * 保存扩展点
		 */
		if (null != extensions && !extensions.isEmpty()) {
			for (BAPExtension bapExtension : extensions) {
				bapExtensionService.save(bapExtension);
			}
		}
		log.info("==================刷新菜单数据完成，耗时" + (System.currentTimeMillis() - currentMill) + " ms");
		String moduleSql = "SELECT * FROM " + Module.TABLE_NAME + " WHERE CODE=?";
		Module m = jdbcTemplate.queryForObject(moduleSql, new Object[]{module.getCode()}, new BeanPropertyRowMapper<Module>(Module.class));
		if(!"sysbase_1.0".equals(module.getCode())){
			actionViewService.refreshModuleActionView(module, env);
		}

		log.info(module.getName() + "上载module.xml花费时间:" + (System.currentTimeMillis() - currentMill) + "ms");
		return m;
	}

	/**
	 * 处理菜单数据
	 * @param parentElement
	 * @param menuInfos
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	private void dealMenuInfos(Element parentElement, List<MenuInfo> menuInfos) throws Exception {
		for (Iterator menuInfosIt = parentElement.elementIterator("menuInfo"); menuInfosIt.hasNext();) {
			Element menuInfoE = (Element) menuInfosIt.next();
			List<MenuOperate> operates = null;
			String code = XmlUtils.getTagContent(menuInfoE.asXML(), "code");
			MenuInfo mi = menuInfoService.get(code);
			if (mi == null) {
				mi = new MenuInfo();
//				mi.setId(IDGenerator.newInstance().generate().longValue());
				mi.setCode(code);
			} else {
				mi.setMenuOperates(new HashSet<MenuOperate>());
				operates = menuOperateService.findMenuOperates(Restrictions.eq("menuInfo.id", mi.getId()));
			}
			for (Iterator menuInfoIt = menuInfoE.elementIterator(); menuInfoIt.hasNext();) {
				Element menuInfoPropertyE = (Element) menuInfoIt.next();
				String menuInfoPropertyName = menuInfoPropertyE.getName();
				if ("parent".equals(menuInfoPropertyName)) {
					if (mi.getId() == null) {
						if (menuInfoPropertyE.elements().size() > 0) {
							resImportMenuInfo(mi, menuInfoPropertyE);
						}
					} else {
						resImportMenuInfo(mi);
					}
					// 操作不处理，已有菜单的操作，在模块发布时更新 
				} else if ("menuOperates".equals(menuInfoPropertyName)) {
					for (Iterator menuOperatesIt = menuInfoPropertyE.elementIterator("menuOperate"); menuOperatesIt.hasNext();) {
						Element menuOperateE = (Element) menuOperatesIt.next();
						MenuOperate mo = null;
						code = XmlUtils.getTagContent(menuOperateE.asXML(), "code");
						if (operates != null && !operates.isEmpty()) {
							for (MenuOperate op : operates) {
								if (code != null && code.equals(op.getCode())) {
									mo = op;
									break;
								}
							}
						}
						if (mo == null) {
							mo = new MenuOperate();
						}
						for (Iterator menuOperateIt = menuOperateE.elementIterator(); menuOperateIt.hasNext();) {
							Element menuOperatePropertyE = (Element) menuOperateIt.next();
							String menuOperatePropertyName = menuOperatePropertyE.getName();
							String menuOperatePropertyValue = menuOperatePropertyE.getTextTrim();
							if (!"cid".equals(menuOperatePropertyName)) {
								if (mo.getId() == null) {
									if (menuOperatePropertyValue.length() > 0) {
										Class type = PropertyUtils.getPropertyType(mo, menuOperatePropertyName);
										if (type != null) {
											PropertyUtils.setProperty(mo, menuOperatePropertyName,
													getObjectValue(type, menuOperatePropertyValue));
										}
									}
								}
							}
						}
						mo.setMenuInfo(mi);
						mi.getMenuOperates().add(mo);
					}
				} else if (!"cid".equals(menuInfoPropertyName) && !"groupOnly".equals(menuInfoPropertyName)) {
					// MenuInfo简单属性
					String menuInfoPropertyValue = menuInfoPropertyE.getTextTrim();
					if ("sort".equals(menuInfoPropertyName)){
						menuInfoPropertyValue = menuInfoPropertyValue.replace(",","");
					}
					if (menuInfoPropertyValue.length() > 0) {
						Class type = PropertyUtils.getPropertyType(mi, menuInfoPropertyName);
						if (type != null) {
							PropertyUtils.setProperty(mi, menuInfoPropertyName,
									getObjectValue(type, menuInfoPropertyValue));
						}
					}
				}
			}
			if(null == mi.getId() && null == mi.getSort()){
				mi.setSort(10000d);
			}
			menuInfos.add(mi);
		}
	}
	
	/**
	 * 更新module.xml数据到env环境的表中
	 * 
	 * @param
	 *
	 * @param
	 * @throws Exception
	 */
	private void synchronizeObjInfo(Map<String, List<Map<String, Object>>> metaMap, Module module, Session session, String... env) throws Exception {
		Long startTime = System.currentTimeMillis();
		Map<String, Map<String, Object>> metaInfo = null;
		Map<String, Map<String, Map<String, Object>>> metaInfoTask = null;
		if("sysbase_1.0".equals(module.getCode())){
			metaInfo = fetchMetaInfoInModule(module, session);
		}else{
			// ???
			metaInfoTask = EcUtils.metaInfoTasksQueue.poll(5, TimeUnit.SECONDS);
			if(null==metaInfoTask){
				EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.metaInfoTasknull"));
				metaInfo = fetchMetaInfoInModule(module, session);
			}else{
				if(metaInfoTask.get("result").get("result").toString() != null && metaInfoTask.get("result").get("result").toString().equals("fail")){
					throw new EcException(InternationalResource.get("ec.model.upload.metaerror"));
				}
				if(metaInfoTask.get(module.getCode()) != null){
					EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.metaInfoTask"));
					metaInfo = metaInfoTask.get(module.getCode());
				}else{
					EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.fetchdata"));
					metaInfo = fetchMetaInfoInModule(module, session);
				}
			}
			
		}
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.querytime")+((System.currentTimeMillis()-startTime)*1.0/1000));
		startTime = System.currentTimeMillis();
		// TODO 将来再完善(处理field)
		Map<String, Object> existsFields = metaInfo.get(Field.class.getName());//获取已经存在的视图字节信息
		List<Map<String, Object>> uploadViews = metaMap.get(View.class.getName());//获取从module.xml中获取需要上载的视图信息
		List<Map<String, Object>> uploadDataGrids = metaMap.get(DataGrid.class.getName());//获取从module.xml中获取需要上载的datagrid信息
		Set<String> uploadViewCodes = new HashSet<>();//把module.xml中的的视图集中在这个集合中
		for (Map<String, Object> item : uploadViews) {
			uploadViewCodes.add((String) item.get("code"));
		}
		Set<String> uploadDataGridCodes = new HashSet<>();//把module.xml中的的集中在这个集合中
		for (Map<String, Object> item : uploadDataGrids) {
			uploadDataGridCodes.add((String) item.get("code"));
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
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.comparemetadata")+((System.currentTimeMillis()-startTime)*1.0/1000));
		startTime = System.currentTimeMillis();
		for (Map.Entry<String, List<Map<String, Object>>> entryItem : metaMap.entrySet()) {
			List<Object []> deletes = new ArrayList<>();
			Map<String, TreeMap<String, Object>> inserts = new LinkedHashMap<String, TreeMap<String, Object>>();
			Map<String, TreeMap<String, Object>> updates = new LinkedHashMap<String, TreeMap<String, Object>>();
			String entityName = entryItem.getKey();
			List<Map<String, Object>> values = entryItem.getValue();
			Map<String, Object> existsInDB = metaInfo.get(entityName);
			for (Map<String, Object> item : values) {
				String code = (String) item.get("code");
				//对map进行key排序
				TreeMap<String, Object> treeMap= new TreeMap<String, Object>();
				for(Map.Entry<String, Object> en:item.entrySet()){
					treeMap.put(en.getKey()	,en.getValue());
				}
				if (existsInDB != null) {
					if (existsInDB.containsKey(code)) {
						updates.put(code, treeMap);
					} else {
						if (entityName.equals(Property.class.getName()) && code.contains("_")) {
							//SQL模型历史数据Property.code可能是全小写，规则变成驼峰，需要把历史数据删除
							if (existsInDB.containsKey(code.substring(0, code.lastIndexOf("_")) + code.substring(code.lastIndexOf("_")).toLowerCase())) {
								Object[] del = {code.toLowerCase()};
								deletes.add(del);
							}
						}
						inserts.put(code, treeMap);
					}
				} else {
					inserts.put(code, treeMap);
				}
			}
			values.clear();
			
			boolean isEc = false;
			if (null != env && env[0].equalsIgnoreCase("ec")) {
				isEc = true;
			}
			
			Map<String, String> sqlMapForInsert=new HashMap<String, String>();
			Map<String, List<List<Object>>> paramsListForInsert=new LinkedHashMap<String, List<List<Object>>>();
			Map<String, List<String>> customFieldMapForInsert=new LinkedHashMap<String, List<String>>();
			Map<String, List<List<String>>> cusLobForInsert=new LinkedHashMap<String, List<List<String>>>();
			
			Map<String, String> sqlMapForUpdate=new HashMap<String, String>();
			Map<String, List<List<Object>>> paramsListForUpdate=new LinkedHashMap<String, List<List<Object>>>();
			Map<String, List<String>> customFieldMapForUpdate=new LinkedHashMap<String, List<String>>();
			Map<String, List<List<String>>> cusLobForUpdate=new LinkedHashMap<String, List<List<String>>>();
			if (entityName.equals(Property.class.getName()) && !deletes.isEmpty()) {
				String delSql = "delete from ec_property where code =?";
				jdbcTemplate.batchUpdate(delSql, deletes);
				session.flush();
			}
			if (inserts != null && !inserts.isEmpty()) {
				//对map中key排序，为了保证sql一直
				//TreeMap
				for (Map.Entry<String, TreeMap<String, Object>> entry : inserts.entrySet()) {
					
					
					batchInsertObjects(Class.forName(entityName), entry.getValue(), isEc,sqlMapForInsert,paramsListForInsert,customFieldMapForInsert,cusLobForInsert);
					//insertObject(Class.forName(entityName), entry.getValue(), isEc);
				}
				
				batchDealInsert(sqlMapForInsert,paramsListForInsert,customFieldMapForInsert,cusLobForInsert);
				session.flush();
			}
			inserts.clear();
			if (updates != null && !updates.isEmpty()) {
				
				for (Map.Entry<String, TreeMap<String, Object>> entry : updates.entrySet()) {
					//if(entityName.equals(Button.class.getName())){
					//	updateObject(Class.forName(entityName), entry.getValue(), isEc);
					//}
					batchUpdateObject(Class.forName(entityName), entry.getValue(), isEc,sqlMapForUpdate,paramsListForUpdate,customFieldMapForUpdate,cusLobForUpdate);
					//updateObject(Class.forName(entityName), entry.getValue(), isEc);
				}
				batchDealInsert(sqlMapForUpdate,paramsListForUpdate,customFieldMapForUpdate,cusLobForUpdate);
				session.flush();
			}
			updates.clear();
		}
		EcUtils.uploadFullLogger.info(InternationalResource.get("ec.model.upload.circularinsertion")+((System.currentTimeMillis()-startTime)*1.0/1000));
	}

	

	private void batchUpdateObject(Class<?> entityClass, Map<String, Object> obj, Boolean isEc, Map<String, String> sqlMap, Map<String, List<List<Object>>> paramsList, Map<String, List<String>> customFieldMap, Map<String, List<List<String>>> cusLobs) throws Exception {

		Object code = null;
		TreeMap<String, Object> tmp = new TreeMap<>();
		if (entityClass.equals(Field.class)) {
			tmp.put("field", obj.get("field"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("field");
			
		}else if (entityClass.equals(Button.class)) {
			tmp.put("button", obj.get("button"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("button");
		}
		//补全缺失的属性默认为null
		checkProInitNull(entityClass,obj);
		
		if (entityClass.equals(Button.class)) {
			if("false".equals(obj.get("isSignatureConfig"))){
				obj.put("isSignatureConfig",true);
			}
		}
			
		StringBuilder builder = new StringBuilder("update ");
		StringBuilder argsColumns = new StringBuilder();
		StringBuilder argsWheres = new StringBuilder();
		String entityName = entityClass.getSimpleName();
		String tableName = (String) reflectService.getStaticFieldValue(entityClass, "TABLE_NAME");
		if (null == tableName || tableName.trim().length() == 0) {
			tableName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().tableize("EC", entityName).toUpperCase();
		}
		if (null == isEc || !isEc) {
			for (String ename : entities) {
				if (entityName.equals(ename)) {
					tableName = tableName.toUpperCase().replaceFirst("EC_", "RUNTIME_");
					break;
				}
			}
		}
		builder.append(tableName + " set ");
		final LinkedList<Object> updateValues = new LinkedList<Object>();
		final LinkedList<String> updateColumnNames = new LinkedList<String>();
		final LinkedList<String> updateLobs = new LinkedList<String>();
		final LinkedList<Object> codeValues = new LinkedList<Object>();
		
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			String fieldName = entry.getKey();
			java.lang.reflect.Field field = reflectService.getDeepField(entityClass, fieldName);
			if (null != field) {
				Transient transient1 = field.getAnnotation(Transient.class);
				if (null != transient1) {
					continue;
				}
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
					columnName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().columnize(fieldName).toUpperCase();
				}
				Lob lob = field.getAnnotation(Lob.class);
				if (null != lob) {
					updateLobs.add(columnName);
				}
				if (entry.getKey() != null && !"code".equals(entry.getKey())) {
					argsColumns.append("," + columnName + "= ?");
					updateColumnNames.addLast(columnName);
					updateValues.addLast(entry.getValue());
				}  
				if (entry.getKey() != null && "code".equals(entry.getKey())) {
					code = entry.getValue();
					argsWheres.append(" where ").append(columnName).append(" = ?");
					codeValues.addLast(entry.getValue());
				}
				if (entry.getKey() != null && "name".equals(entry.getKey())) {
					if(ArrayUtils.contains(ENTITIECLASSNAMES, entityClass.getName())){
						argsColumns.append(",value_zh_cn = ? ");
						updateColumnNames.addLast("value_zh_cn");
						updateValues.addLast(InternationalResource.get(entry.getValue().toString()));
					} 
				}
			}
		}
		if (argsColumns.length() > 0 && argsWheres.length() > 0) {
			if (ReflectUtils.isExtends(entityClass, AbstractAuditUniqueCodeEntity.class) &&  !updateColumnNames.contains("MODIFY_TIME")) {
				argsColumns.append(",MODIFY_TIME = ? ");
				updateColumnNames.addLast("MODIFY_TIME");
				updateValues.addLast(new Date());
			}
			updateValues.addLast(codeValues.getFirst());
			updateColumnNames.addLast("code");
			builder.append(argsColumns.substring(1)).append(argsWheres);
			
			if(sqlMap==null){
				sqlMap=new HashMap<String, String>();
			}
			
			if(cusLobs==null){
				cusLobs=new HashMap<String, List<List<String>>>();
			}
			if(paramsList==null){
				paramsList=new HashMap<String, List<List<Object>>>();
			}
			
			if(customFieldMap==null){
				customFieldMap=new HashMap<String, List<String>>();
			}
			
			sqlMap.put(entityName, builder.toString());
			if(paramsList.get(entityName)!=null){
				paramsList.get(entityName).add(updateValues);
			}else{
				List<List<Object>> newList=new LinkedList<List<Object>>();
				newList.add(updateValues);
				paramsList.put(entityName, newList);
			}
			customFieldMap.put(entityName, updateColumnNames);
			
			if(cusLobs.get(entityName)!=null){
				cusLobs.get(entityName).add(updateLobs);
			}else{
				List<List<String>> clobList=new LinkedList<List<String>>();
				clobList.add(updateLobs);
				cusLobs.put(entityName, clobList);
			}
			
		}
			
			
	}
	
		
	private void batchDealInsert(Map<String, String> sqlMap, Map<String, List<List<Object>>> paramsList, Map<String, List<String>> customFieldMap, Map<String, List<List<String>>> cusLob){
		if(sqlMap.isEmpty()){
			return;
		}
		for(Map.Entry<String, String> entry:sqlMap.entrySet()){
			String entityClass=entry.getKey();
			String sql=entry.getValue();
			
			final List<List<Object>> params=paramsList.get(entityClass);
			final List<String> columnNames=customFieldMap.get(entityClass);
			final List<List<String>> clobs=cusLob.get(entityClass);
			
			
			jdbcTemplate.batchUpdate(sql,  new BatchPreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					List<Object> values = params.get(i);

					List<String> clobbb=clobs.get(i);
					for (int j = 1; j <= columnNames.size(); j++) {
 						Object value = values.get(j - 1);

						if (clobbb.contains(columnNames.get(j - 1))) {
							String clobValue = value == null ? "" : value.toString();
							ps.setCharacterStream(j, new StringReader(clobValue), clobValue.length());

						} else if (value instanceof String) {
							ps.setString(j, value.toString());
						} else if (value instanceof Integer) {
							ps.setInt(j, Integer.parseInt(value.toString()));
						} else if (value instanceof Long) {
							ps.setLong(j, Long.parseLong(value.toString()));
						} else if (value instanceof Boolean) {
							ps.setInt(j, Boolean.parseBoolean(value.toString()) ? 1 : 0);
						} else if (value instanceof BigDecimal) {
							ps.setBigDecimal(j, new BigDecimal(value.toString()));
						} else if (value instanceof Double) {
							ps.setDouble(j, Double.parseDouble(value.toString()));
						} else if (value instanceof Float) {
							ps.setFloat(j, Float.parseFloat(value.toString()));
						} else if (value instanceof Short) {
							ps.setShort(j, Short.parseShort(value.toString()));
						} else if (value instanceof Date) {
							ps.setTimestamp(j, new Timestamp(new Date().getTime()));
						} else if (value instanceof AbstractCodeEntity) {
							Object code = null;
							try {
								code = reflectService.getFieldValue(value, "code");
							} catch (Exception e) {
							}
							if (null != code) {
								ps.setString(j, code.toString());
							} else {
								ps.setString(j, "");
							}
						} else if (value instanceof AbstractIdEntity) {
							Object id = null;
							try {
								id = reflectService.getFieldValue(value, "id");
							} catch (Exception e) {
							}
							if (null != id) {
								ps.setLong(j, Long.parseLong(id.toString()));
							} else {
								ps.setString(j, null);
							}
						} else if (value instanceof AbstractCodeEntity) {
							Object id = null;
							try {
								id = reflectService.getFieldValue(value, "id");
							} catch (Exception e) {
							}
							if (null != id) {
								ps.setString(j, id.toString());
							} else {
								ps.setString(j, null);
							}
						} else if (value instanceof Enum) {
							ps.setString(j, value.toString());
						}else{

							ps.setObject(j, value);
						}
					}


				}

				@Override
				public int getBatchSize() {
					return params.size();
				}
			});

		}
		
	}

	public static final String[] entities = new String[] { "Module", "ModuleRelation", "Entity", "Model", "Property", "View", "DataGroup", "DataClassific",
			"Sql", "DataGrid", "ExtraView", "FastQueryJson", "AdvQueryJson", "BackupView", "ExtraQueryJson", "Field", "Button", "Event",
			"Validate", "BackupDataGrid","CustomerCondition","ModuleReference", "PrintTemplate","ImportTemplate","SchedulerJob", "Echarts", "EchartsModel","ModuleLIMS", "EntityLIMS", "ModelLIMS",
			"PropertyLIMS", "ViewLIMS", "DataGroupLIMS", "DataClassificLIMS","SqlLIMS", "DataGridLIMS", "ExtraViewLIMS", "FastQueryJsonLIMS", "AdvQueryJsonLIMS",
			"BackupViewLIMS", "ExtraQueryJsonLIMS", "FieldLIMS", "ButtonLIMS", "EventLIMS","ValidateLIMS", "BackupDataGridLIMS","CustomerConditionLIMS","PrintTemplateLIMS"};


	//组织批量更新的sql
	private void batchInsertObjects(Class<?> entityClass, Map<String, Object> obj, Boolean isEc, Map<String, String> sqlMap, Map<String, List<List<Object>>> paramsList, Map<String, List<String>> customFieldMap, Map<String, List<List<String>>> cusLobs) throws Exception {

		if (obj == null || obj.size() == 0) {
			return;
		}
		// setObject
		Map<String, Object> tmp = new HashMap<>();
		if (entityClass.equals(Field.class)) {
			tmp.put("field", obj.get("field"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("field");
			
		}
		if (entityClass.equals(Button.class)) {
			tmp.put("button", obj.get("button"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("button");
		}
		obj.put("version", 0);
		//补全缺失的属性默认为null
		checkProInitNull(entityClass,obj);
		
		if (entityClass.equals(Button.class)) {
			if("false".equals(obj.get("isSignatureConfig"))){
				obj.put("isSignatureConfig", true);
			}
			if("RESTORE".equals(obj.get("operateType"))){
				obj.put("isSignatureConfig", false);
			}
		}
		
		StringBuilder builder = new StringBuilder("insert into ");
		String entityName = entityClass.getSimpleName();
		java.lang.reflect.Field table_name = entityClass.getField("TABLE_NAME");

		String tableName = String.valueOf(table_name.get(entityName));
		if (null == tableName || tableName.trim().length() == 0) {
			tableName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().tableize("EC", entityName).toUpperCase();
		}
		if (null == isEc || !isEc) {
			for (String ename : entities) {
				if (entityName.equals(ename)) {
					tableName = tableName.toUpperCase().replaceFirst("EC_", "RUNTIME_");
					break;
				}
			}
		}
		builder.append(tableName);
		StringBuilder params = new StringBuilder();
		StringBuilder fields = new StringBuilder();
		final LinkedList<Object> values = new LinkedList<Object>();
		final LinkedList<String> columnNames = new LinkedList<String>();
		final LinkedList<String> lobs = new LinkedList<String>();
		
		Object instance = entityClass.newInstance();
		SystemCode layerCode = null;
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			if (entry.getKey() != null ) {
				try {
					if (entityClass.equals(Field.class) && "layerType".equals(entry.getKey()) && entry.getValue() != null) {
						layerCode = new SystemCode();
						layerCode.setId(entry.getValue().toString());
						PropertyUtils.setProperty(instance, entry.getKey(), layerCode);
					} else if (entry.getValue() != null
							&& (entry.getValue().getClass().equals(List.class)
							|| entry.getValue().getClass().equals(ArrayList.class))
							&& (Set.class.equals(entityClass.getDeclaredField(entry.getKey()).getType())
							|| HashSet.class.equals(entityClass.getDeclaredField(entry.getKey()).getType()))) {
						Set set = new HashSet<>(Arrays.asList(entry.getValue()));
						PropertyUtils.setProperty(instance, entry.getKey(), set);
					} else {
						PropertyUtils.setProperty(instance, entry.getKey(), entry.getValue());
					}
				} catch (NoSuchMethodException e) {
					if (entityClass.equals(Field.class)) {
						log.warn(e.getMessage(),e);
					}
				} catch (NullPointerException e) {
					log.error("空指针 instance=" + instance + "entry.getKey()=" + entry.getKey() + "entry.getValue()" + entry.getValue());
				}
			}
		}
		List<java.lang.reflect.Field> fieldList = ReflectUtils.getDeepDeclaredFields(entityClass);
		
		if (null != fieldList) {
			for(Iterator<java.lang.reflect.Field> iter = fieldList.iterator(); iter.hasNext();){
				java.lang.reflect.Field field=iter.next();
				String fieldName = field.getName();
				
				if (!Modifier.isStatic(field.getModifiers()) && !obj.containsKey(fieldName)) {
					
					Method method = null;
					try {
  						method = reflectService.getMethod(entityClass, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
					} catch (NoSuchMethodException e) {
						if (field.getType() == boolean.class) {
							try {
								method = reflectService.getMethod(entityClass, "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1),
										null, null);
							} catch (Exception e2) {
							}
						}
					}
					if (null != method){
					Transient transient1 = method.getAnnotation(Transient.class);
					if (null != transient1) {
						continue;
					}
					if (!ReflectUtils.isCollection(method.getReturnType())) {
						Object defaultValue = method.invoke(instance);
						
						if (null != defaultValue) {
							obj.put(fieldName, defaultValue);
						}else{
							if(fieldName.equalsIgnoreCase("CREATE_TIME")){
								obj.put(fieldName, new Date());
							}else{
								obj.put(fieldName, null);
							}
						}
					}
				}}
			}
		}
		
		//组织字段、字段值，通过反射去取字段名
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
		
			if (entry.getKey() != null ) {
				String fieldName = entry.getKey();
				java.lang.reflect.Field field = reflectService.getDeepField(entityClass, fieldName);
				
//				Method method = null;
//				try {
//					method = reflectService.getMethod(entityClass, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
//				} catch (NoSuchMethodException e) {
//					if (field!=null&&field.getType() == boolean.class) {
//						try {
//							method = reflectService.getMethod(entityClass, "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null,
//									null);
//						} catch (Exception e2) {
//							log.warn(e.getMessage(), e);
//						}
//					}
//				}
				
				if (null != field) {
					
					//去掉Transient的字段
					Transient transient2 = field.getAnnotation(Transient.class);
					if (null != transient2) {
						continue;
					}

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
						columnName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().columnize(fieldName).toUpperCase();
					}
					Lob lob = field.getAnnotation(Lob.class);
					if (null != lob) {
						lobs.addLast(columnName);
					}
					
					columnNames.addLast(columnName);
					fields.append("," + columnName);
					params.append(",?");
					values.addLast(entry.getValue());
					if (entry.getKey() != null && "name".equals(entry.getKey())) {
						if(ArrayUtils.contains(ENTITIECLASSNAMES, entityClass.getName())){
							columnNames.addLast("value_zh_cn");
							fields.append(",value_zh_cn ");
							params.append(",?");
							values.addLast(InternationalResource.get(entry.getValue().toString()));
						} 
					}
				}
			}
		}
		if ( fields.length() > 0) {
//			if (ReflectUtils.isExtends(entityClass, AbstractCodeEntity.class) && !columnNames.contains("CREATE_TIME")) {
//				columnNames.addLast("CREATE_TIME");
//				fields.append(",CREATE_TIME");
//				params.append(",?");
//				values.addLast(new Date());
//			}
			
			builder.append(" (").append(fields.substring(1)).append(")").append(" values (").append(params.substring(1)).append(")");
			
			//log.info("builder============"+builder.toString());
			//log.info("columnNames============"+columnNames.toString());
			//log.info("lobs============"+lobs.toString());
			//log.info("values============"+values.toString());
			if(sqlMap==null){
				sqlMap=new HashMap<String, String>();
			}
			
			if(cusLobs==null){
				cusLobs=new HashMap<String, List<List<String>>>();
			}
			if(paramsList==null){
				paramsList=new HashMap<String, List<List<Object>>>();
			}
			
			if(customFieldMap==null){
				customFieldMap=new HashMap<String, List<String>>();
			}
			
			sqlMap.put(entityName, builder.toString());
			if(paramsList.get(entityName)!=null){
				paramsList.get(entityName).add(values);
			}else{
				List<List<Object>> newList=new LinkedList<List<Object>>();
				newList.add(values);
				paramsList.put(entityName, newList);
			}
			customFieldMap.put(entityName, columnNames);
			
			if(cusLobs.get(entityName)!=null){
				cusLobs.get(entityName).add(lobs);
			}else{
				List<List<String>> clobList=new LinkedList<List<String>>();
				clobList.add(lobs);
				cusLobs.put(entityName, clobList);
			}
			
		}
	
		
	}

	/**
	 * 同步数据
	 * 
	 * @param
	 *
	 * @param
	 * @throws Exception
	 */
	private void synchronizeObjInfo(Map<String, List<Map<String, Object>>> metaMap, Module module, Session session) throws Exception {
		this.synchronizeObjInfo(metaMap, module, session, "runtime");
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
		if (entityClass.equals(Field.class)) {
			tmp.put("field", obj.get("field"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("field");
		}
		if (entityClass.equals(Button.class)) {
			tmp.put("button", obj.get("button"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("button");
		}
		//有缺失的属性默认为null
		checkProInitNull(entityClass,obj);
		StringBuilder builder = new StringBuilder("insert into ");
		String entityName = entityClass.getSimpleName();
		String tableName = (String) reflectService.getStaticFieldValue(entityClass, "TABLE_NAME");
		if (null == tableName || tableName.trim().length() == 0) {
			tableName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().tableize("EC", entityName).toUpperCase();
		}
		if (null == isEc || !isEc) {
			for (String ename : entities) {
				if (entityName.equals(ename)) {
					tableName = tableName.toUpperCase().replaceFirst("EC_", "RUNTIME_");
					break;
				}
			}
		}
		builder.append(tableName);
		StringBuilder params = new StringBuilder();
		StringBuilder fields = new StringBuilder();
//		final LinkedList<Class> types = new LinkedList<Class>();
		final LinkedList<Object> values = new LinkedList<Object>();
		final LinkedList<String> columnNames = new LinkedList<String>();
		final LinkedList<String> lobs = new LinkedList<String>();
		Object instance = entityClass.newInstance();
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				try {
					PropertyUtils.setProperty(instance, entry.getKey(), entry.getValue());
				} catch (NoSuchMethodException e) {
					if (entityClass.equals(Field.class)) {
						log.warn(e.getMessage(),e);
					}
				}
			}
		}
		List<java.lang.reflect.Field> fieldList = ReflectUtils.getDeepDeclaredFields(entityClass);
		if (null != fieldList) {
			for (java.lang.reflect.Field field : fieldList) {
				String fieldName = field.getName();
				if (!Modifier.isStatic(field.getModifiers()) && !obj.containsKey(fieldName)) {
					Transient transient1 = field.getAnnotation(Transient.class);
					if (null != transient1) {
						continue;
					}
					Method method = null;
					try {
						method = reflectService.getMethod(entityClass, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
					} catch (NoSuchMethodException e) {
						if (field.getType() == boolean.class) {
							try {
								method = reflectService.getMethod(entityClass, "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1),
										null, null);
							} catch (Exception e2) {
							}
						}
					}
					if(null != method)
						transient1 = method.getAnnotation(Transient.class);
					if (null != transient1) {
						continue;
					}
					if (null != method && !ReflectUtils.isCollection(method.getReturnType())) {
						Object defaultValue = method.invoke(instance);
						if (null != defaultValue) {
							obj.put(fieldName, defaultValue);
						}
					}
				}
			}
		}
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				String fieldName = entry.getKey();
				java.lang.reflect.Field field = reflectService.getDeepField(entityClass, fieldName);
//				Method method = null;
//				try {
//					method = reflectService.getMethod(entityClass, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
//				} catch (NoSuchMethodException e) {
//					if (field.getType() == boolean.class) {
//						try {
//							method = reflectService.getMethod(entityClass, "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null,
//									null);
//						} catch (Exception e2) {
//							log.warn(e.getMessage(), e);
//						}
//					}
//				}
				if (null != field) {
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
						columnName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().columnize(fieldName).toUpperCase();
					}
					Lob lob = field.getAnnotation(Lob.class);
					if (null != lob) {
						lobs.add(columnName);
					}
					columnNames.add(columnName);
					fields.append("," + columnName);
					params.append(",?");
					values.add(entry.getValue());
				}
			}
		}
		if (fields.length() > 0) {
			if (instance instanceof AbstractCodeEntity) {
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
						} else if (value instanceof AbstractCodeEntity) {
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
	 * @param session
	 *            TODO
	 * @param obj
	 * @param
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	@Deprecated
	private void insertObject(Session session, Class<?> entityClass, Map<String, Object> obj) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
		if (obj == null || obj.size() == 0) {
			return;
		}
		// setObject
		Object objSaved = entityClass.newInstance();
		Map<String, Object> tmp = new HashMap<>();
		if (entityClass.equals(Field.class)) {
			tmp.put("field", obj.get("field"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("field");
		}
		if (entityClass.equals(Button.class)) {
			tmp.put("button", obj.get("button"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("button");
		}
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				try {
					PropertyUtils.setProperty(objSaved, entry.getKey(), entry.getValue());
				} catch (NoSuchMethodException e) {
					if (entityClass.equals(Field.class)) {
						log.warn(e.getMessage(),e);
					}
				}
			}
		}
		session.save(objSaved);
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
		
		checkProInitNull(entityClass,obj);
		StringBuilder builder = new StringBuilder("update ");
		StringBuilder argsColumns = new StringBuilder();
		StringBuilder argsWheres = new StringBuilder();
		String entityName = entityClass.getSimpleName();
		String tableName = (String) reflectService.getStaticFieldValue(entityClass, "TABLE_NAME");
		if (null == tableName || tableName.trim().length() == 0) {
			tableName = com.supcon.supfusion.configuration.services.utils.Inflector.getInstance().tableize("EC", entityName).toUpperCase();
		}
		if (null == isEc || !isEc) {
			for (String ename : entities) {
				if (entityName.equals(ename)) {
					tableName = tableName.toUpperCase().replaceFirst("EC_", "RUNTIME_");
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
			java.lang.reflect.Field field = reflectService.getDeepField(entityClass, fieldName);
//			Method method = null;
//			try {
//				method = reflectService.getMethod(entityClass, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
//			} catch (NoSuchMethodException e) {
//				if (field.getType() == boolean.class) {
//					try {
//						method = reflectService.getMethod(entityClass, "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), null, null);
//					} catch (Exception e2) {
//						log.warn(e.getMessage(), e);
//					}
//				}
//			}
			if (null != field) {
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
		if (argsColumns.length() > 0 && argsWheres.length() > 0) {
			if (ReflectUtils.isExtends(entityClass, AbstractCodeEntity.class)) {
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
							if(value==null){value="";}
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
						} else if (value instanceof Enum) {
							ps.setString(i, value.toString());
						}
					}
				}
			});
		}
	}

	/**
	 * 
	 * @param session
	 *            TODO
	 * @param obj
	 * @param
	 */
	@Deprecated
	private void updateObject(Session session, String entityName, Map<String, Object> obj) {
		if (obj == null || obj.size() == 0) {
			return;
		}
		StringBuffer sql = new StringBuffer();
		List<Object> args = new ArrayList<Object>();
		StringBuffer argsColumns = new StringBuffer();
		StringBuffer argsWheres = new StringBuffer();
		Object code = null;
		Map<String, Object> tmp = new HashMap<>();
		if (entityName.equals(getEntityName(Field.class.getName()))) {
			tmp.put("field", obj.get("field"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("field");
		}
		if (entityName.equals(getEntityName(Button.class.getName()))) {
			tmp.put("button", obj.get("button"));
			obj.put("config", SerializeUitls.serializeAsXml(tmp));
			obj.remove("button");
		}
		sql.append("update " + entityName + " set ");
		for (Map.Entry<String, Object> entry : obj.entrySet()) {
			if (entry.getKey() != null && !"code".equals(entry.getKey())) {
				if (entry.getValue() != null) {
					argsColumns.append((argsColumns.length() > 0 ? "," : "") + entry.getKey() + " = ? ");
					args.add(entry.getValue());
				} else {
					argsColumns.append((argsColumns.length() > 0 ? "," : "") + entry.getKey() + " = null ");
				}
			} else if (entry.getKey() != null && "code".equals(entry.getKey())) {
				argsWheres.append(" where " + entry.getKey() + " = ?");
				code = entry.getValue();
			}
		}
		if (args.isEmpty()) {
			return;
		}
		args.add(code);
		sql.append(argsColumns).append(argsWheres);
		createQuery(session, sql.toString(), args.toArray(new Object[args.size()])).executeUpdate();
	}

	private String getEntityName(String fullClassName) {
		if (fullClassName.indexOf(".") > 0) {
			return fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
		} else {
			return fullClassName;
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> fetchMetaInfo(Session session, Class<? extends AbstractCodeEntity> clazz, Criterion... criterions) {
		Criteria criteria = session.createCriteria(clazz);
		if (criterions != null && criterions.length > 0) {
			for (Criterion criterion : criterions) {
				criteria.add(criterion);
			}
		}
		List<? extends AbstractCodeEntity> result = criteria.list();
		if (result != null && !result.isEmpty()) {
			Map<String, Object> retMap = new HashMap<>(result.size());
			for (Object item : result) {
				AbstractCodeEntity tmp = (AbstractCodeEntity) item;
				retMap.put(tmp.getCode(), item);
			}
			return retMap;
		}
		return new HashMap<>();
	}

	/**
	 * 
	 * @param module
	 * @return
	 */
	private Map<String, Map<String, Object>> fetchMetaInfoInModule(Module module, Session session) {
		if (module == null) {
			return null;
		}
		Map<String, Map<String, Object>> retMap = new HashMap<>();
		Map<String, Object> tmpMap = new HashMap<>();
		Criterion criterion = Restrictions.or(Restrictions.like("code", module.getCode(), MatchMode.START));
//				Restrictions.like("code", "_" + module.getCode() + "_", MatchMode.ANYWHERE));
		Criterion criterionSysBaseProperty = Restrictions.or(Restrictions.like("code", "sysbase_1.0", MatchMode.START),
				Restrictions.like("code", "base_", MatchMode.START));
		for (EcEntityEnum enumItem : EcEntityEnum.values()) {
			if (enumItem == EcEntityEnum.Property && module.getCode().equals("sysbase_1.0")) {
				tmpMap = fetchMetaInfo(session, enumItem.clazz, criterionSysBaseProperty);
			} else {
				tmpMap = fetchMetaInfo(session, enumItem.clazz, criterion);
			}
			retMap.put(enumItem.clazz.getName(), tmpMap);
		}
		return retMap;
	}
	
	/**
	 * 添加元数据的二级缓存
	 * @param
	 */
	
	private void getAllMetaInfoIn(Session session) {
		Map<String, Map<String, Object>> retMap = new HashMap<>();
		Map<String, Object> tmpMap = new HashMap<>();
		Criterion criterion = Restrictions.isEmpty("code");
		for (EcEntityEnum enumItem : EcEntityEnum.values()) {
			Criteria criteria = session.createCriteria(enumItem.clazz);
			criteria.list();
		}
	}



	private Map<String, Object> getLastMetaItem(Map<String, List<Map<String, Object>>> metaMap, String typeName) {
		List<Map<String, Object>> ret = metaMap.get(typeName);
		if (ret == null) {
			ret = new ArrayList<>();
			ret.add(new HashMap<String, Object>());
			metaMap.put(typeName, ret);
		}
		return ret.get(ret.size() - 1);
	}

	private Map<String, Object> getLastMetaItemAdd(Map<String, List<Map<String, Object>>> metaMap, String typeName) {
		List<Map<String, Object>> ret = metaMap.get(typeName);
		if (ret == null) {
			ret = new ArrayList<>();
			metaMap.put(typeName, ret);
		}
		ret.add(new HashMap<String, Object>());
		return ret.get(ret.size() - 1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getObjectValue(Class type, String val) throws Exception {
		if (type.isEnum())
			return Enum.valueOf(type, val);
		return ConvertUtils.convert(val, type);
	}

	/**
	 * Attempts to locate the specified field on the class.
	 * 
	 * @param clazz
	 *            the class definition containing the field
	 * @param fieldName
	 *            the name of the field to locate
	 * 
	 * @return the Field (never null)
	 * 
	 * @throws IllegalStateException
	 *             if field could not be found
	 */
	public java.lang.reflect.Field getField(Class<?> clazz, String fieldName) throws IllegalStateException {
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object getObjectValue(Class clazz, String name, String val) throws Exception {
		Class type = fieldTypeMap.get(clazz.getName() + "$$" + name);
		if (type == null) {
			try {
				java.lang.reflect.Field field = getField(clazz, name);
				type = field.getType();
				fieldTypeMap.put(clazz.getName() + "$$" + name, type);
			} catch (IllegalStateException e) {
				return null;
			}
		}
		if (type.isEnum()){
			try {
				return Enum.valueOf(type, val);
			} catch (Exception e) {
			}
			return null;
		}
			
		return ConvertUtils.convert(val, type);
	}

	@SuppressWarnings("rawtypes")
	private boolean checkIfPropertyExists(Class clazz, String name) throws Exception {
		try {
			if (errorFieldTypeMap.contains(clazz.getName() + "$$" + name)) {
				return false;
			}
			Class type = fieldTypeMap.get(clazz.getName() + "$$" + name);
			if (type == null) {
				java.lang.reflect.Field field = getField(clazz, name);
				type = field.getType();
				fieldTypeMap.put(clazz.getName() + "$$" + name, type);
			}
		} catch (Exception e) {
			errorFieldTypeMap.add(clazz.getName() + "$$" + name);
			return false;
		}
		return true;
	}

	private void resImportMenuInfo(MenuInfo mi) {
		mi.setParent(menuInfoService.getParent(mi.getId()));
		if (mi.getParent() != null && mi.getParent().getId() != -1L) {
			resImportMenuInfo(mi.getParent());
		}
	}

	@SuppressWarnings("rawtypes")
	private void resImportMenuInfo(MenuInfo mi, Element e) throws Exception {
		String code = XmlUtils.getTagContent(e.asXML(), "code");
		MenuInfo parent = menuInfoService.get(code);
		if (parent == null) {
			parent = new MenuInfo();
		}
		for (Iterator menuInfoIt = e.elementIterator(); menuInfoIt.hasNext();) {
			Element menuInfoPropertyE = (Element) menuInfoIt.next();
			String menuInfoPropertyName = menuInfoPropertyE.getName();
			if ("parent".equals(menuInfoPropertyName)) {
				if (parent.getId() == null) {
					if (menuInfoPropertyE.elements().size() > 0) {
						resImportMenuInfo(parent, menuInfoPropertyE);
					}
				} else {
					resImportMenuInfo(parent);
				}

			} else {
				// MenuInfo简单属性
				if(null == parent.getId()){
					String menuInfoPropertyValue = menuInfoPropertyE.getTextTrim();
					if (menuInfoPropertyValue.length() > 0 && !"cid".equals(menuInfoPropertyName)) {
						if (parent.getId() == null) {
							Class type = PropertyUtils.getPropertyType(parent, menuInfoPropertyName);
							if (type != null) {
								PropertyUtils.setProperty(parent, menuInfoPropertyName, getObjectValue(type, menuInfoPropertyValue));
							}
						}
					}
				}
			}
		}
		if(null == parent.getId() && null == parent.getSort()){
			parent.setSort(10000d);
		}
		if (mi.getCode() != null && !mi.getCode().equals(parent.getCode())) {
			mi.setParent(parent);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
	}

	private boolean isRuntime(String... env) {
		return env == null || env.length == 0 || (!"ec".equals(env[0]) && !"proj".equals(env[0]));
	}
	//会丢失部分参数，需补全
	private void checkProInitNull(Class<?> entityClass, Map<String, Object> obj){
		
		if (entityClass.equals(Field.class)) {
			//会丢失部分参数，需补全
			for(String s : Constants.FIELD_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
			}
		}else if (entityClass.equals(Button.class)) {
			//会丢失部分参数，需补全
			for(String s : Constants.BUTTON_PRO){
				if(!obj.containsKey(s)){
					if(s.equals("isConfirm")  || s.equals("isUseMore")  || s.equals("isPermission") || s.equals("isCallback") || s.equals("isCustomFunc") || s.equals("isHide") || s.equals("isPublished")){
						obj.put(s, false);
					}else if(s.equals("isSignatureConfig")){
						obj.put(s, true);
					} else {
						obj.put(s, null);
					}
					
				}
			}
			
		}else if(entityClass.equals(Event.class)){
			//会丢失部分参数，需补全
			for(String s : Constants.EVENT_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
				
			}
		}else if(entityClass.equals(Validate.class)){
			
			//会丢失部分参数，需补全
			for(String s : Constants.VALIDATE_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
				
			}
		}else if(entityClass.equals(CustomerCondition.class)){
			
			//会丢失部分参数，需补全
			for(String s : Constants.CUSTOMCONDITION_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
			}
		}else if(entityClass.equals(SpecialPermission.class)){
			
			//会丢失部分参数，需补全
			for(String s : Constants.SPECIALPERMISSION_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
			}
		}else if(entityClass.equals(Script.class)){
			
			//会丢失部分参数，需补全
			for(String s : Constants.SCPRIPT_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
			}
		}else if(entityClass.equals(DataGrid.class)){
			
			//会丢失部分参数，需补全
			for(String s : Constants.DATAGRID_PRO){
				if(!obj.containsKey(s)){
					obj.put(s, null);
				}
			}
		}
	}
	
	public void saveMenuInfo(Module module, List<MenuInfo> menuInfos, String... env) {
		// 保存菜单
		Map<String, MenuInfo> menuMap = new HashMap<String, MenuInfo>();
		Long companyId = getCurrentCompanyId();
		if (null == companyId) {
			if (null != isSingleMode && isSingleMode) {
				Company company = companyService.getCompanyByCode(defaultCompanyCode);
				if (null == company) {
					companyId = getCurrentCompanyId();
				}
			}
		}
		if (null == companyId) {
			if (EcUtils.uploadTask.get("curCompany") != null && !"null".equals(EcUtils.uploadTask.get("curCompany"))) {
				companyId = Long.valueOf(EcUtils.uploadTask.get("curCompany"));
			} else {
				companyId = Long.valueOf(1L);
			}
		}
		//保存菜单前更新国际化缓存,用于助记码保存
		internationalService.refreshI18n();
		if (menuInfos != null && !menuInfos.isEmpty()) {
			List<MenuInfo> saveMenuInfos = new ArrayList<>(menuInfos.size());
			try {
				for (MenuInfo mi : menuInfos) {
					Stack<MenuInfo> stack = new Stack<MenuInfo>();
					MenuInfo p = mi;
					while (null != p) {
						stack.push(p);
						if (p.getParent() != null && !p.getParent().getCode().equals(p.getCode())) {
							p = p.getParent();
						} else {
							p = null;
						}
					}
					p = null;
					while (!stack.isEmpty()) {
						MenuInfo menu = stack.pop();
						//app字段在保存的时候处理，这里暂时不处理 modifyby yuanyang
//						if (!StringUtils.isEmpty(module.getCode())) {
//							menu.setApp(module.getCode().split("_")[0]);
//						} else {
//							menu.setApp(menu.getCode().split("_")[0]);
//						}
						menu.setParent(p);
						if (null != p) {
							menu.setParentId(p.getId());
							menu.setParentCode(p.getCode());
						}
						if (null == menu.getCid()) {
							menu.setCid(companyId);
						}
						if (!menuMap.containsKey(menu.getCode())) {
							try {
								if (menu.getMenuOperates() != null && menu.getMenuOperates().isEmpty()) {
									menu.setMenuOperates(Collections.unmodifiableSet(Collections.EMPTY_SET));
								}
							} catch (LazyInitializationException e) {
								menu.setMenuOperates(Collections.unmodifiableSet(Collections.EMPTY_SET));
							}

							if(null == menu.getModuleCode() || menu.getModuleCode().equals(module.getArtifact())){
								menu.setModuleCode(module.getCode());
							}
//							if (menu.getId() == null) {
//								menu.setId(IDGenerator.newInstance().generate().longValue());
//							}
							if(ifSupos) {
					        	//融合环境上载的菜单状态为2
								menu.setStatus(2);
					        }else {       	
					        	menu.setStatus(0);
					        }
							saveMenuInfos.add(menu);
						
							customMenuInfoService.save(menu);
							menuMap.put(menu.getCode(), menu);

							MenuInfo p1 = menu.getParent();
							if (p1 != null && menu.getId().equals(p1.getId()) ) {
								p1 = null;
							}

							String layRec = menu.getId() + "";
							String fullPathName = OrchidUtils.getMainDisplayValue(menu);

							while (null != p1 && p1.getId() != null && p1.getId() != -1) {
								layRec = p1.getId() + "-" + layRec;
								fullPathName = OrchidUtils.getMainDisplayValue(p1) + "/" + fullPathName;
								if(p1.getLeaf()){
									p1.setLeaf(false);
									if(!ifSupos) {
										if("icon-set".equals(p1.getCssClass()) || "".equals(p1.getCssClass())){
											p1.setCssClass("icon-folder");
										}
									}else {
										p1.setStatus(2);
									}
									saveMenuInfos.add(p1);
									customMenuInfoService.save(p1);
								}
								if (p1.getParent() != null && !(p1.getId() .equals(p1.getParent().getId()))) {
									p1 = p1.getParent();
								} else {
									p1 = null;
								}
							}
							menu.setLayRec(layRec);
							if (menu.getMenuOperates() != null && !menu.getMenuOperates().isEmpty()) {
								for (MenuOperate mo : menu.getMenuOperates()) {
									if (null == mo.getCid()) {
										mo.setCid(companyId);
									}
									mo.setMenuInfo(menu);
									mo.setVersion(0);
									if(mo.getId()==null){
										menuOperateService.save(mo);
									}
								}
							}
						} else {
							menu = menuMap.get(menu.getCode());
						}

						p = menu;
					}
				}
//				menuInfoService.saveMenuInfoAndOperates(saveMenuInfos);
			} catch (Exception e) {
				EcUtils.uploadLogger.info("<span style='color:red'>菜单上载失败，请查看菜单服务日志</span>");
				EcUtils.uploadFullLogger.info("<span style='color:red'>菜单上载失败，请查看菜单服务日志</span>");
				log.error(e.getMessage(), e);
			}

		}
	}
}
