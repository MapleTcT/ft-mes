/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowFormat;
import com.supcon.supfusion.configuration.services.dao.*;
import com.supcon.supfusion.configuration.services.service.DataProcessingService;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 实体配置 数据处理使用
 * 
 * @author zhuyuyin
 * @version 1.0
 */
@Slf4j
@ServiceApiService("ec_DataProcessingService")
public class DataProcessingServiceImpl implements DataProcessingService {
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private EntityService entityService;
	@Autowired
	private ViewDaoImpl viewDao;
	@Autowired
	private ModelDaoImpl modelDao;
	@Autowired
	private FieldDaoImpl fieldDao;
	@Autowired
	private DataGridDaoImpl dataGridDao;
	@Autowired
	private DataGroupDaoImpl dataGroupDao;
	@Autowired
	private DataClassificDaoImpl dataClassificDao;
	@Autowired
	private EventDaoImpl eventDao;
	@Autowired
	private ButtonDaoImpl buttonDao;
	@Autowired
	private ValidateDaoImpl validateDao;
	@Autowired
	private CustomerConditionDaoImpl customerConditionDao;
	@Autowired
	private PropertyDaoImpl propertyDao;
	/**
	 * 给继承{@link }的对象添加属性
	 */
	@Transactional
	@Override
	public void addModuleCodeToAllObject() {
		List<Module> modules = moduleService.findAllModules();
		if (null != modules && !modules.isEmpty()) {
			for (Module module : modules) {
				String moduleCode = module.getCode();
				List<Entity> entities = entityService.findEntities(module);
				if (null != entities && !entities.isEmpty()) {
					for (Entity entity : entities) {
						String entityCode = entity.getCode();
						List<Model> models = modelDao.findByCriteria(Restrictions.eq("entity.code", entity.getCode()));
						List<View> views = viewDao.findByCriteria(Restrictions.eq("entity.code", entity.getCode()));
						if (null != models && !models.isEmpty()) {
							String updateModelSQL = "update Model set moduleCode=?  where entity.code=?";
							modelDao.bulkExecute(updateModelSQL, moduleCode, entityCode);

							for (Model model : models) {
								String updatePropertySQL = "update ec_property set MODULE_CODE=? , ENTITY_CODE=? where MODEL_CODE=?";
								propertyDao.createNativeQuery(updatePropertySQL, moduleCode, entityCode, model.getCode()).executeUpdate();
							}
						}
						if (null != views && !views.isEmpty()) {
							String updateViewSQL = "update View set moduleCode=?  where entity.code=?";
							viewDao.bulkExecute(updateViewSQL, moduleCode, entityCode);
							for (View view : views) {
								String dgSql = "update ec_data_grid set MODULE_CODE=?, ENTITY_CODE=? where VIEW_CODE=?";
								String fieldSQL = "update ec_field set MODULE_CODE=? , ENTITY_CODE=? where VIEW_CODE=? or DATAGRID_CODE like ?";
								String dataGroupSQL = "update ec_data_group set MODULE_CODE=? , ENTITY_CODE=? where VIEW_CODE=?";
								String dataClassificSQL = "update ec_data_classific set MODULE_CODE=? , ENTITY_CODE=? where DATA_GROUP_CODE like ?";
								String conditionSQL = "update ec_customer_condition set MODULE_CODE=? , ENTITY_CODE=? where VIEW_CODE=? or DATAGRID_CODE like ? or DATACLASSIFIC_CODE like ?";
								String buttonSQL = "update ec_button set MODULE_CODE=? , ENTITY_CODE=? where VIEW_CODE=? or DATAGRID_CODE like ?";
								String eventSQL = "update ec_event set MODULE_CODE=? , ENTITY_CODE=? where CODE like ?";
								String validateSQL = "update ec_validate set MODULE_CODE=? , ENTITY_CODE=? where FIELD_CODE like ?";

								dataGridDao.createNativeQuery(dgSql, moduleCode, entityCode, view.getCode()).executeUpdate();

								fieldDao.createNativeQuery(fieldSQL, moduleCode, entityCode, view.getCode(), view.getCode() + "%")
										.executeUpdate();
								dataGroupDao.createNativeQuery(dataGroupSQL, moduleCode, entityCode, view.getCode()).executeUpdate();
								dataClassificDao.createNativeQuery(dataClassificSQL, moduleCode, entityCode, view.getCode() + "%")
										.executeUpdate();
								customerConditionDao.createNativeQuery(conditionSQL, moduleCode, entityCode, view.getCode(),
										view.getCode() + "%", view.getCode() + "%").executeUpdate();
								buttonDao.createNativeQuery(buttonSQL, moduleCode, entityCode, view.getCode(), view.getCode() + "%")
										.executeUpdate();
								eventDao.createNativeQuery(eventSQL, moduleCode, entityCode, view.getCode() + "%").executeUpdate();
								validateDao.createNativeQuery(validateSQL, moduleCode, entityCode, view.getCode() + "%").executeUpdate();
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 初始化fullPathName
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public void initFullPathNameForTree() {
		List<Module> modules = moduleService.findAllModules();
		if (null != modules && !modules.isEmpty()) {
			for (Module module : modules) {
				List<Entity> entities = entityService.findEntities(module);
				if (null != entities && !entities.isEmpty()) {
					for (Entity entity : entities) {
						List<Model> models = modelDao.findByCriteria(Restrictions.eq("entity.code", entity.getCode()));
						if (null != models && !models.isEmpty()) {
							for (Model model : models) {
								if (model.getDataType() == 2) {
									List<Property> properties = propertyDao.findByCriteria(Restrictions.eq("model.code", model.getCode()));
									if(!entity.getIsInherentedBase()) {
										boolean flag = false;
										for(Property p : properties) {
											if(p.getCode().equals(model.getCode() + "_" + "fullPathName")) {
												flag = true;
												break;
											}
										}
										if(!flag) {
											Property property = new Property();
											property.setModel(model);
											property.setName("fullPathName");
											property.setCode(model.getCode() + "_" + "fullPathName");
											property.setDisplayName("ec.common.fullPathName");
											property.setValid(true);
											property.setIsIndex(false);
											property.setIsInherent(true);
											property.setNullable(true);
											property.setIsUnique(false);
											property.setIsPk(false);
											property.setIsUsedForList(true);
											property.setType(DbColumnType.TEXT);
											if (model.getIsMain()) {
												property.setAssociatedType(null);
												property.setAssociatedProperty(null);
											}
											property.setFormat(ShowFormat.TEXT);
											property.setFieldType(FieldType.TEXTFIELD);
											property.setModuleCode(model.getModuleCode());
											property.setEntityCode(model.getEntity().getCode());
											propertyDao.save(property);
										}
									}
									
									String tableName = model.getTableName();
									String sql = "SELECT ID, LAY_REC FROM " + tableName;
									String mainDisplayProp = "id";
									for(Property p : properties) {
										if(p.getIsMainDisplay()) {
											mainDisplayProp = p.getColumnName();
											break;
										}
									}
									String nameSql = "SELECT " + mainDisplayProp + " FROM " + tableName + " WHERE ID = ?";
									String updateSql = "UPDATE " + tableName + " SET FULL_PATH_NAME = ? WHERE ID = ?";
									try {
										List<Object[]> list = propertyDao.createNativeQuery(sql).list();
										for(Object[] obj : list) {
											if(null != obj[1]) {
												String layRec = obj[1].toString();
												if(layRec.length() > 0) {
													String[] layRecs = layRec.split("-");
													StringBuilder fullPathNameSb = new StringBuilder();
													for(String lr : layRecs) {
														List<Object> nameList = propertyDao.createNativeQuery(nameSql, lr).list();
														if(null != nameList && !nameList.isEmpty()) {
															fullPathNameSb.append(null == nameList.get(0) ? "" : nameList.get(0));
															fullPathNameSb.append("/");
														}
													}
													String fullPathName = "";
													if(null != fullPathNameSb && fullPathNameSb.length() > 0) {
														fullPathName = fullPathNameSb.substring(0, fullPathNameSb.length() - 1).toString();
													}
													propertyDao.createNativeQuery(updateSql, new Object[] {fullPathName, obj[0]}).executeUpdate();
												}
											}
										}
									} catch(SQLGrammarException e) {
										log.warn(e.getMessage(), e);
										continue;
									}
								}
							}
						}
					}
				}
			}
		}
	}	
	
	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public void dealListPtForAssId() {
		String sql = "SELECT CODE, FIELD_KEY, PROPERTY_CODE, FULL_PROPERTY_CODE, VIEW_CODE FROM EC_FIELD WHERE REGION_TYPE = 'LISTPT' AND VALID = 1 AND IS_HIDDEN = 1";
		List<Object[]> fields = fieldDao.createNativeQuery(sql).list();
		if(null != fields && !fields.isEmpty()) {
			for(Object[] field : fields) {
				if(null != field[1]) {
					String fieldKey = field[1].toString();
					if(fieldKey.endsWith(".id")) {
						String propertyCode = null;
						if(null != field[2]) {
							propertyCode = field[2].toString();
						}
						if(null != field[4]) {
							String viewCode = field[4].toString();
							if(null != viewCode && viewCode.length() > 0) {
								String modelSql = "SELECT M.CODE FROM EC_VIEW V, EC_MODEL M WHERE V.ASS_MODEL_CODE = M.CODE AND V.CODE = ?";
								Object modelCode = fieldDao.createNativeQuery(modelSql, viewCode).uniqueResult();
								if(null != modelCode && !modelCode.toString().startsWith("sysbase_1.0")) {
									String truePropertyCode = modelCode.toString() + "_id";
									if(!truePropertyCode.equals(propertyCode)) {
										propertyCode = truePropertyCode;
									}
								} else {
									if(!propertyCode.endsWith("_id")) {
										propertyCode = propertyCode.substring(0, propertyCode.lastIndexOf("_") + 1) + "id";
									}
								}
							} 
						}
						String fullPathPropertyCode = "";
						if(null != field[3]) {
							fullPathPropertyCode = field[3].toString();
							if(null != fullPathPropertyCode && fullPathPropertyCode.length() > 0 && fullPathPropertyCode.indexOf("||") > 0) {
								fullPathPropertyCode = fullPathPropertyCode.substring(0, fullPathPropertyCode.lastIndexOf("||") + 2) + propertyCode;
							}
						}
						String updateSql = "update ec_field set PROPERTY_CODE = ? , FULL_PROPERTY_CODE = ? where CODE = ?";
						fieldDao.createNativeQuery(updateSql, new Object[] {propertyCode, fullPathPropertyCode, field[0]}).executeUpdate();
					}
				}
			}
		}
	}	
	
	/**
	 * 初始化version字段
	 */
	@Transactional
	@Override
	public void initVersionProperty() {
		List<Module> modules = moduleService.findAllModules();
		if (null != modules && !modules.isEmpty()) {
			for (Module module : modules) {
				if (!(null != module.getIsInherentedBase() && module.getIsInherentedBase())) {
					List<Entity> entities = entityService.findEntities(module);
					if (null != entities && !entities.isEmpty()) {
						for (Entity entity : entities) {
							List<Model> models = modelDao.findByCriteria(Restrictions.eq("entity.code", entity.getCode()));
							if (null != models && !models.isEmpty()) {
								for (Model model : models) {
									Property property = propertyDao.findEntityByCriteria(Restrictions.eq("code", model.getCode()
											+ "_version"));
									if (null == property) {
										property = new Property();
										property.setCode(model.getCode() + "_version");
										property.setModel(model);
										property.setName("version");
										property.setDisplayName("ec.common.version");
										property.setValid(true);
										property.setIsIndex(false);
										property.setIsInherent(true);
										property.setNullable(true);
										property.setIsUnique(false);
										property.setIsPk(false);
										property.setIsUsedForList(true);
										property.setType(DbColumnType.INTEGER);
										if (model.getIsMain()) {
											property.setAssociatedType(null);
											property.setAssociatedProperty(null);
										}
										property.setFormat(ShowFormat.TEXT);
										property.setFieldType(FieldType.TEXTFIELD);

										property.setModuleCode(module.getCode());
										property.setEntityCode(entity.getCode());
										propertyDao.save(property);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	@Transactional
	public void initLeafForTree() {
		List<Module> modules = moduleService.findAllModules();
		if (null != modules && !modules.isEmpty()) {
			for (Module module : modules) {
				List<Entity> entities = entityService.findEntities(module);
				if (null != entities && !entities.isEmpty()) {
					for (Entity entity : entities) {
						List<Model> models = modelDao.findByCriteria(Restrictions.eq("entity.code", entity.getCode()));
						if (null != models && !models.isEmpty()) {
							for (Model model : models) {
								if (model.getDataType() == 2) {

									String tableName = model.getTableName();
									try {
										// 把没有没有后代的节点leaf设为true
										String sql1 = "update " + tableName
												+ " t1 set t1.leaf=1 where not exists (select t2.id from " + tableName
												+ " t2 where t2.parent_id = t1.id and t2.valid = 1) and t1.leaf is null";
										propertyDao.createNativeQuery(sql1).executeUpdate();
										// 把有后代的节点leaf设为false
										String sql2 = "update " + tableName + " t1 set t1.leaf=0 where exists (select t2.id from "
												+ tableName + " t2 where t2.parent_id = t1.id and t2.valid = 1) and t1.leaf is null";
										propertyDao.createNativeQuery(sql2).executeUpdate();
									} catch (SQLGrammarException e) {
										log.warn(e.getMessage(), e);
										continue;
									}
								}
							}
						}
					}
				}
			}
		}
	}
			
}
