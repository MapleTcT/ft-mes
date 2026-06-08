/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 

 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.FieldDaoImpl;
import com.supcon.supfusion.configuration.services.dao.SelectionRangeDaoImpl;
import com.supcon.supfusion.configuration.services.dao.ViewDaoImpl;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.*;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.configuration.services.utils.StringUtils;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Hibernate;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhuyuyin
 * @version $Id$
 */
@Slf4j
@ServiceApiService("ec_FieldService")
@Transactional
public class FieldServiceImpl implements FieldService {

	private static final String CACHE_NAME = "ec_field";
	private static final String CACHE_FIELD_PREFIX = "cache_field";
	private static Map<String, Set<String>> keys = new ConcurrentHashMap<String, Set<String>>();
	private static final String[] FIELD_CONSTANT = { "key", "name", "isHidden", "displayName", "showType", "showFormat", "layRec", "none",
			"regionType", "columnType" };

	@Autowired
	private FieldDaoImpl fieldDao;
	
	@Autowired
	private ViewDaoImpl viewDao;
	
	@Autowired
	private SelectionRangeDaoImpl selectionRangeDao;

	@Autowired
	private ModelService modelService;

	@Autowired
	private EventService eventService;

	@Autowired
	private ValidateService validateService;

	@Autowired
	private DataGridService dataGridService;

	@Autowired
	private ViewService viewService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ModuleService moduleService;

	@Override
	@Transactional
	public void saveField(Field field) {
		if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
			field.setProjFlag(true);
		}
		fieldDao.save(field);
		String assCode = null;
		if (null != field.getView() && field.getView().getCode().length() > 0) {
			assCode = field.getView().getCode();
		} else if (null != field.getDataGrid() && field.getDataGrid().getCode().length() > 0) {
			assCode = field.getDataGrid().getCode();
		}
//		cache.remove(CACHE_FIELD_PREFIX + field.getCode());
//		cache.remove(CACHE_FIELD_PREFIX + assCode);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Field getFieldByDgCode(String dgCode, String viewCode) {
		List<Field> fields = fieldDao.findByHql("from Field where view.code = ?0 and showType = 'DATAGRID' ", viewCode);
		if (null != fields && !fields.isEmpty()) {
			for (Field f : fields) {
				String config = f.getConfig();
				if (null != config && config.length() > 0) {
					Map<String, Object> map = (Map<String, Object>) SerializeUitls.deserialize(config);
					Map<String, Object> fieldMap = (Map<String, Object>) map.get("field");
					if (null != fieldMap && !fieldMap.isEmpty()) {
						if (null != fieldMap.get("DataGridCode") && fieldMap.get("DataGridCode").toString().length() > 0
								&& fieldMap.get("DataGridCode").toString().equals(dgCode)) {
							return f;
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Field getField(String fieldCode) {
		Field field = fieldDao.findEntityByHql("from Field where code = ?0 and valid = true", fieldCode);
//		Element element = cache.get(CACHE_FIELD_PREFIX + fieldCode);
//		if (null != element) {
//			field = (Field) element.getObjectValue();
//		} else {
			field = fieldDao.load(fieldCode);
//			cache.put(new Element(CACHE_FIELD_PREFIX + fieldCode, field));
//		}
		return field;
	}

	@Override
	@Transactional
	public void deleteField(Field field) {
		try {
			eventService.deleteEventByField(field.getCode());
			fieldDao.deletePhysical(field);
		} catch (Exception e) {
			log.info("该field异常"+field.getCode()+e.getMessage());
		}
	}

	@Override
	@Transactional
	public void deleteField(String fieldCode) {
		Field field = getField(fieldCode);
		if (null != field) {
			fieldDao.deletePhysical(field);
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Field> getFieldByPropertyCode(String propertyCode) {
		return fieldDao.findByCriteria(Restrictions.eq("property.code", propertyCode));
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Field> getFieldByPropertyCodeLike(String propertyCode){
		return fieldDao.findByCriteria(Restrictions.like("fullPropertyCode", propertyCode, MatchMode.START));
	}

	@Override
	@Transactional
	public void deleteFieldByDataGrid(String dgCode) {
		List<Field> fields = getFieldsByDataGridCode(dgCode);
		if (fields != null && !fields.isEmpty()) {
			for (Field field : fields) {
				eventService.deleteEventByField(field.getCode());
				deleteField(field);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional
	public void deleteFieldByCellCodes(String code, String cellCodes) {
		fieldDao.flush();
		if (cellCodes != null && cellCodes.length() > 0) {
			String[] cellCodeArr = cellCodes.split(",");
			for (String cellCode : cellCodeArr) {
				Field f = fieldDao.findEntityByCriteria(Restrictions.like("code", code, MatchMode.START), Restrictions.eq("cellCode", cellCode));
				if (null != f) {
					if (f.getShowType() == FieldType.DATAGRID) {
						String config = f.getConfig();
						if (config != null && config.length() > 0) {
							Map configMap = (Map) SerializeUitls.deserialize(config);
							if (null != configMap && !configMap.isEmpty()) {
								if (null != configMap.get("field")) {
									Map<String, Object> fieldInfo = (Map<String, Object>) configMap.get("field");
									if (null != fieldInfo.get("DataGridCode") && fieldInfo.get("DataGridCode").toString().length() > 0) {
										String dgCode = fieldInfo.get("DataGridCode").toString();
										dataGridService.deleteDataGridPhysical(dgCode);
									}
								}
							}
						}

					}
					if (f.getRegionType() == RegionType.FASTQUERY) {
						Field assFastField = fieldDao.findEntityByCriteria(Restrictions.eq("view.code", f.getView().getCode()),
								Restrictions.eq("property.code", (null == f.getProperty()) ? null : f.getProperty().getCode()),
								Restrictions.eq("fullPropertyCode", f.getFullPropertyCode()),
								Restrictions.eq("regionType", RegionType.FASTQUERY), Restrictions.eq("none", "none"));
						if (null != assFastField) {
							deleteField(assFastField);
						}
					}
					if (f.getRegionType() == RegionType.ADVQUERY) {
						Field assFastField = fieldDao.findEntityByCriteria(Restrictions.eq("view.code", f.getView().getCode()),
								Restrictions.eq("property.code", (null == f.getProperty()) ? null : f.getProperty().getCode()),
								Restrictions.eq("fullPropertyCode", f.getFullPropertyCode()),
								Restrictions.eq("regionType", RegionType.ADVQUERY), Restrictions.eq("none", "none"));
								if (null != assFastField) {
									deleteField(assFastField);
						}
					}
					if (f.getRegionType() == RegionType.LISTPT) {
						Field assListField;
						if(f.getView() != null ){
							assListField = fieldDao.findEntityByCriteria(Restrictions.eq("view.code", f.getView().getCode()),
									Restrictions.eq("property.code", (null == f.getProperty()) ? null : f.getProperty().getCode()),
									Restrictions.eq("fullPropertyCode", f.getFullPropertyCode()),
									Restrictions.eq("regionType", RegionType.LISTPT), Restrictions.eq("none", "hide"));
							if (null != assListField) {
								deleteField(assListField);
							}
						}else if(f.getDataGrid() != null){
							assListField = fieldDao.findEntityByCriteria(Restrictions.eq("dataGrid.code", f.getDataGrid().getCode()),
									Restrictions.eq("property.code", (null == f.getProperty()) ? null : f.getProperty().getCode()),
									Restrictions.eq("fullPropertyCode", f.getFullPropertyCode()),
									Restrictions.eq("regionType", RegionType.LISTPT), Restrictions.eq("none", "hide"));
							if (null != assListField) {
								deleteField(assListField);
							}
						}
						
					}
					deleteSelectionRangeByField(f);
					deleteField(f);
				}
			}
			fieldDao.flush();
		}
	}

	private String getFieldCode(String code, Map<String, Object> map) {
		String fieldCode = code + "_" + map.get("regionType").toString() + "_";
		if (null != map.get("propertyCode") && map.get("propertyCode").toString().length() > 0) {
			if (null != map.get("showType") && "LABEL".equals(map.get("showType").toString())) {
				fieldCode += "LABEL_";
			} else {
				fieldCode += "OTHER_";
			}
			String propertyCode = map.get("propertyCode").toString();

			fieldCode += propertyCode.replace("||", "_");

			if (("LISTPT".equals(map.get("regionType").toString()) || "DATAGRID".equals(map.get("regionType").toString()))
					&& null != map.get("none")) {
				fieldCode += "_none";
			}
			if ("FASTQUERY".equals(map.get("regionType").toString())) {
				String key = "";
				if (null != map.get("key") && map.get("key").toString().length() > 0) {
					key = map.get("key").toString();
				} else if (null != map.get("name") && map.get("name").toString().length() > 0) {
					key = map.get("name").toString();
				}
				if (key != null && key.length() > 0) {
					if (key.indexOf("_start") > -1) {
						fieldCode += "_start";
					} else if (key.indexOf("_end") > -1) {
						fieldCode += "_end";
					}
				}
				if (null != map.get("layoutname")) {		//增强型视图查询条件，code加上布局名以去重   by fukun
					fieldCode += "_" + map.get("layoutname").toString();
				}
			}
		} else {
			if (null != map.get("DataGridCode") && map.get("DataGridCode").toString().length() > 0) {
				String dgCode = map.get("DataGridCode").toString();
				fieldCode += dgCode.replaceAll("\\.", "_");
			} else if (map.get("assoFlag") != null && "true".equalsIgnoreCase(map.get("assoFlag").toString())) {
				String assCode = (String) map.get("code");
				if (assCode != null && assCode.length() > 0) {
					fieldCode = assCode;
				} else {
					fieldCode += "ASSO_" + UUID.randomUUID().toString().replace("-", "_"); // Freemarker中-为运算符，替换成_
					map.put("key", "attrMap." + fieldCode.replace(".", "_"));
				}
			} else if (map.get("customSection") != null && "true".equalsIgnoreCase(map.get("customSection").toString())) {
				String customCode = (String) map.get("code");
				if (customCode != null && customCode.length() > 0) {
					fieldCode = customCode;
				} else {
					fieldCode += "CUSTOM_" + UUID.randomUUID().toString().replace("-", "_"); // Freemarker中-为运算符，替换成_
					map.put("key", fieldCode.replace(".", "_"));
				}
			} else {
				if (null != map.get("key") && map.get("key").toString().length() > 0) {
					String key = map.get("key").toString();
					fieldCode += key.replaceAll("\\.", "_");
				} else if (null != map.get("name") && map.get("name").toString().length() > 0) {
					String key = map.get("name").toString();
					fieldCode += key.replaceAll("\\.", "_");
				}
			}
		}
		return fieldCode;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	@Override
	public void saveFields(Object object, String fieldConfig, String delCellIds, String delEventIds, String delValidateIds) {
		String objCode = "", objField = "";
		if (object instanceof View) {
			View view = (View) object;
			objCode = view.getCode();
			objField = "VIEW_CODE";
			if (view.getIsShadow() != null && view.getIsShadow()) {
                return;
            }
		} else if (object instanceof DataGrid) {
			DataGrid dataGrid = (DataGrid) object;
			objCode = dataGrid.getCode();
			objField = "DATAGRID_CODE";
			if (dataGrid.getView() != null && dataGrid.getView().getIsShadow() != null && dataGrid.getView().getIsShadow()) {
                return;
            }
		} else if (object instanceof FastQueryJson) {
			FastQueryJson fastQueryJson = (FastQueryJson) object;
			objCode = fastQueryJson.getCode();
			objField = "FASTQUERYJSON_CODE";
			if (fastQueryJson.getView() != null && fastQueryJson.getView().getIsShadow() != null && fastQueryJson.getView().getIsShadow()) {
                return;
            }
		} else if (object instanceof AdvQueryJson){
			AdvQueryJson advQueryJson = (AdvQueryJson) object;
			objCode = advQueryJson.getCode();
			objField = "ADVQUERYJSON_CODE";
			if(advQueryJson.getView() != null && advQueryJson.getView().getIsShadow() != null && advQueryJson.getView().getIsShadow()) {
                return;
            }
		}
		if (delCellIds != null && delCellIds.length() > 0) {
			deleteFieldByCellCodes(objCode, delCellIds);
		}
		if (delEventIds != null && delEventIds.length() > 0) {
			String[] delEventCodes = delEventIds.split(",");
			for (String delEventCode : delEventCodes) {
				eventService.deleteEvent(delEventCode);
			}
		}
		if (delValidateIds != null && delValidateIds.length() > 0) {
			String[] delValidateCodes = delValidateIds.split(",");
			for (String delValidateCode : delValidateCodes) {
				validateService.deleteValidate(delValidateCode);
			}
		}
		fieldDao.flush();
		if (fieldConfig != null && !fieldConfig.isEmpty()) {
			// long sychronizeCurrent = System.currentTimeMillis();
			// log.info("===字段序列化为Map开始:" + sychronizeCurrent);
//			cache.remove(CACHE_FIELD_PREFIX + objCode);
			Map fieldsMap = (Map) SerializeUitls.deserialize(fieldConfig);
			// log.info("===字段序列化为Map结束，花费时间:" + (System.currentTimeMillis() - sychronizeCurrent) + "ms");
			if (fieldsMap != null && !fieldsMap.isEmpty()) {
				List<Map> fields = (List<Map>) fieldsMap.get("fields");
				List<String> dgCodes = new ArrayList<String>();
				Map<String, String> allFieldMap = new HashMap<String, String>();
				if (fields != null && !fields.isEmpty()) {
					for (int i = 0; i < fields.size(); i++) {
						Map<String, Object> map = fields.get(i);
						Map newMap = new HashMap();
						newMap.put("field", map);
						if (null != map.get("DataGridCode") && map.get("DataGridCode").toString().length() > 0) {
							dgCodes.add(map.get("DataGridCode").toString());
						}
						saveField(object, newMap, allFieldMap);
					}
					fieldDao.flush();
					if (object instanceof View) {
						View view = (View) object;
						// 删除列上fieldCode与生成的fieldCode不同的字段
						deleteFieldByCellCodeAndExcludeFieldCode(view.getCode(), allFieldMap);
					}
					if (null != allFieldMap && !allFieldMap.isEmpty()) {
						int perTime = 999;
						List<String> codes = new ArrayList<String>();
						List<String> ids = new ArrayList<String>();
						StringBuilder prefix = new StringBuilder("SELECT CODE FROM EC_FIELD WHERE (CELL_CODE" + fieldDao.getConcatSymbol()
								+ "'--'" + fieldDao.getConcatSymbol() + " CODE)");
						if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
							prefix = prefix.replace(0, prefix.length(), "SELECT CODE FROM PROJECT_FIELD WHERE (CELL_CODE" + fieldDao.getConcatSymbol()
									+ "'--'" + fieldDao.getConcatSymbol() + " CODE)");
						}
						StringBuilder suffix = new StringBuilder(" AND ").append(objField).append(" = '").append(objCode).append("'");
						for (Entry<String, String> entry : allFieldMap.entrySet()) {
							ids.add("('" + entry.getKey() + "--" + entry.getValue() + "')");
						}
						int count = (ids.size() / perTime) + (ids.size() % perTime == 0 ? 0 : 1);
						for (int i = 0; i < count; i++) {
							if (i > 0) {
								prefix.append(" OR ");
							}
							prefix.append(" NOT IN (");
							List<String> subList = ids.subList(perTime * i, perTime * i
									+ (i < count - 1 ? perTime : (ids.size() % perTime)));
							for (int g = 0; g < subList.size(); g++) {
								if (g != 0) {
									prefix.append(",");
								}
								prefix.append(subList.get(g));
							}
							prefix.append(" ) ");
						}
						prefix.append(suffix);
						log.info(prefix.toString());
						codes = fieldDao.createNativeQuery(prefix.toString()).list();
						if (null != codes && !codes.isEmpty()) {
							for (String code : codes) {
								deleteField(code);
							}
						}
					}
					// log.info("===删除多余字段结束，花费时间:" + (System.currentTimeMillis() - sychronizeMill) + "ms");
					if (object instanceof View) {
						// 将视图中无用的DataGrid删除
						View view = (View) object;
						if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW || view.getType() == ViewType.EXTRA) {
							List<DataGrid> dataGrids = dataGridService.getDataGridByViewCode(view.getCode());
							if (dataGrids != null && !dataGrids.isEmpty()) {
								for (DataGrid dg : dataGrids) {
									if (dgCodes != null && !dgCodes.isEmpty()) {
										boolean isDel = true;
										for (String code : dgCodes) {
											if (dg.getCode().equals(code)) {
												isDel = false;
												break;
											}
										}
										if (isDel) {
											dg.setValid(false);
											dataGridService.save(dg);
											deleteFieldByDataGrid(dg.getCode());
										}

									} else {
										dg.setValid(false);
										dataGridService.save(dg);
										deleteFieldByDataGrid(dg.getCode());
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 保存单个Field
	 *
	 * @param object
	 * @param fieldMap
	 * @param allFieldMap
	 *            TODO
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public void saveField(Object object, Map<String, Object> fieldMap, Map<String, String> allFieldMap) {
		// long sychronizeCurrent = System.currentTimeMillis();
		// log.info("===保存单个Field开始:" + sychronizeCurrent);
		Map<String, Object> map = (Map<String, Object>) fieldMap.get("field");
		// long tempMill = System.currentTimeMillis();
		// log.info("===反序列化开始:" + tempMill);
		String code = "";
		String moduleCode = null;
		String entityCode = null;
		if (object instanceof View) {
			View view = (View) object;
			code = view.getCode();
			moduleCode = view.getModuleCode();
			entityCode = view.getEntity().getCode();
		} else if (object instanceof DataGrid) {
			DataGrid dataGrid = (DataGrid) object;
			code = dataGrid.getCode();
			moduleCode = dataGrid.getModuleCode();
			entityCode = dataGrid.getEntityCode();
		} else if (object instanceof FastQueryJson) {
			FastQueryJson fastQueryJson = (FastQueryJson) object;
			code = fastQueryJson.getCode();
			moduleCode = fastQueryJson.getView().getModuleCode();
			entityCode = fastQueryJson.getView().getEntity().getCode();
			if (fastQueryJson.getView() != null && fastQueryJson.getView().getIsShadow() != null && fastQueryJson.getView().getIsShadow()) {
                return;
            }
		} else if (object instanceof AdvQueryJson){
			AdvQueryJson advQueryJson = (AdvQueryJson) object;
			code = advQueryJson.getCode();
			moduleCode = advQueryJson.getView().getModuleCode();
			entityCode = advQueryJson.getView().getEntity().getCode();
			if(advQueryJson.getView() != null && advQueryJson.getView().getIsShadow() != null && advQueryJson.getView().getIsShadow()) {
                return;
            }
		}
		String fieldCode = code + "_" + map.get("regionType").toString() + "_";
		if (null != map.get("propertyCode") && map.get("propertyCode").toString().length() > 0) {
			if (null != map.get("showType") && "LABEL".equals(map.get("showType").toString())) {
				fieldCode += "LABEL_";
			} else {
				fieldCode += "OTHER_";
			}
			String propertyCode = map.get("propertyCode").toString();

			fieldCode += propertyCode.replace("||", "_");

			if (("LISTPT".equals(map.get("regionType").toString()) || "DATAGRID".equals(map.get("regionType").toString()))
					&& null != map.get("none")) {
				fieldCode += "_none";
			}
			if ("FASTQUERY".equals(map.get("regionType").toString())) {
				String key = "";
				if (null != map.get("key") && map.get("key").toString().length() > 0) {
					key = map.get("key").toString();
				} else if (null != map.get("name") && map.get("name").toString().length() > 0) {
					key = map.get("name").toString();
				}
				if (key != null && key.length() > 0) {
					if (key.indexOf("_start") > -1) {
						fieldCode += "_start";
					} else if (key.indexOf("_end") > -1) {
						fieldCode += "_end";
					}
				}
				if (null != map.get("layoutname")) {		//增强型视图查询条件，code加上布局名以去重   by fukun
					fieldCode += "_" + map.get("layoutname").toString();
				}
			}
		} else {
			if (null != map.get("DataGridCode") && map.get("DataGridCode").toString().length() > 0) {
				String dgCode = map.get("DataGridCode").toString();
				List<String> viewCode = jdbcTemplate.queryForList("select view_code from ec_data_grid where code=? and valid=1", new Object[] { dgCode }, String.class);
				if (null != viewCode && !viewCode.isEmpty() && !viewCode.get(0).equals(code)) {
					return;
				}
				fieldCode += dgCode.replaceAll("\\.", "_");
			} else if (map.get("assoFlag") != null && "true".equalsIgnoreCase(map.get("assoFlag").toString())) {
				String assCode = (String) map.get("code");
				if (assCode != null && assCode.length() > 0) {
					fieldCode = assCode;
				} else {
					fieldCode += "ASSO_" + UUID.randomUUID().toString().replace("-", "_"); // Freemarker中-为运算符，替换成_
					map.put("key", "attrMap." + fieldCode.replace(".", "_"));
				}
			} else if (map.get("customSection") != null && "true".equalsIgnoreCase(map.get("customSection").toString())) {
				String customCode = (String) map.get("code");
				if (customCode != null && customCode.length() > 0) {
					fieldCode = customCode;
				} else {
					fieldCode += "CUSTOM_" + UUID.randomUUID().toString().replace("-", "_"); // Freemarker中-为运算符，替换成_
					map.put("key", fieldCode.replace(".", "_"));
				}
			} else {
				if (null != map.get("key") && map.get("key").toString().length() > 0) {
					String key = map.get("key").toString();
					fieldCode += key.replaceAll("\\.", "_");
				} else if (null != map.get("name") && map.get("name").toString().length() > 0) {
					String key = map.get("name").toString();
					fieldCode += key.replaceAll("\\.", "_");
				}
			}
		}
		if (null != allFieldMap) {
			allFieldMap.put(map.get("cellCode").toString(), fieldCode);
		}
//		fieldDao.flush();
//		fieldDao.clear();
		Field field = getField(fieldCode);
		if (field == null) {
			field = new Field();
			field.setVersion(0);
		}
		field.setModuleCode(moduleCode);
		field.setEntityCode(entityCode);
		// 把新得到的fieldcode放回config中
		map.put("code", fieldCode);
		for (String fc : FIELD_CONSTANT) {
			Class type;
			try {
				type = PropertyUtils.getPropertyType(field, fc);
				Method setMethod = field.getClass().getMethod("set" + Character.toUpperCase(fc.charAt(0)) + fc.substring(1), type);
				if (map.get(fc) != null && !"displayName".equals(fc)) {
					if (null != type
							&& ("FieldType".equals(type.getSimpleName()) || "ShowFormat".equals(type.getSimpleName()) || "RegionType".equals(type
									.getSimpleName())  || "DbColumnType".equals(type.getSimpleName()))) {
						String getmethodName = "get" + Character.toUpperCase(fc.charAt(0)) + fc.substring(1);
						Method getMethod = field.getClass().getMethod(getmethodName);
						Class clazz = getMethod.getReturnType();
						setMethod.invoke(field, Enum.valueOf(clazz, map.get(fc).toString()));
					} else {
						setMethod.invoke(field, map.get(fc));
					}
				} else if ("displayName".equals(fc) && map.get("namekey") != null) {
					field.setDisplayName(map.get("namekey").toString());
				} else {
					setMethod.invoke(field, map.get(fc));
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}

		if (null == map.get("key") && null != map.get("name")
				&& (map.get("customSection") == null || !"true".equalsIgnoreCase(map.get("customSection").toString()))) {
			field.setKey(map.get("name").toString());
		}
		if (null != map.get("propertyCode") && map.get("propertyCode").toString().length() > 0) {
			String propertyCode = map.get("propertyCode").toString();
			if (field.getProperty() != null) {
				Property property = null;
				if (propertyCode.contains("||")) {
					property = modelService.getProperty(propertyCode.substring(propertyCode.lastIndexOf("||") + 2));
				} else {
					property = modelService.getProperty(propertyCode);
				}
				if (null != property) {
					field.setProperty(property);
					// 加入field对应的数据库列名，运行期列排序使用
					map.put("columnName", property.getColumnName());
				}
			}
			field.setFullPropertyCode(propertyCode);
		}
		String config = SerializeUitls.serializeAsXml(fieldMap);
		field.setConfig(config);
		if (object instanceof View) {
			field.setView((View) object);
		} else if (object instanceof DataGrid) {
			field.setDataGrid((DataGrid) object);
		} else if (object instanceof FastQueryJson){
			field.setFastQueryJson((FastQueryJson)object);
		} else if (object instanceof AdvQueryJson){
			field.setAdvQueryJson((AdvQueryJson)object);
		}
		field.setCellCode(map.get("cellCode").toString());
		field.setCode(fieldCode);
		if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
			field.setProjFlag(true);
		}
		// 图层字段，设置图层类型
		if (null != map.get("layerType") && !"".equals(map.get("layerType"))) {
			field.setLayerType(new SystemCode(map.get("layerType").toString()));
		}
		field = fieldDao.merge(field);
//		cache.remove(CACHE_FIELD_PREFIX + field.getCode());
		// log.info("===反射并保存Field，花费时间:" + (System.currentTimeMillis() - dataMill00) + "ms");
		// long dataMill000 = System.currentTimeMillis();
		if (null != map.get("events") && map.get("events") instanceof List) {
			List<Map<String, String>> eventList = (List<Map<String, String>>) map.get("events");
			if (null != eventList && !eventList.isEmpty()) {
				for (Map<String, String> event : eventList) {
					String type = event.get("name").toString().split("=")[0];
					Event e = eventService.getEvent(fieldCode + "_" + type);
					if (null == e) {
						e = new Event();
						e.setVersion(0);
					}
					e.setCode(fieldCode + "_" + type);
					e.setName(event.get("name").toString());
					e.setFunction(event.get("function").toString());
					e.setFunction_es5(event.get("function_es5"));
					e.setField(field);
					e.setModuleCode(moduleCode);
					e.setEntityCode(entityCode);
					eventService.saveEvent(e);
				}
			}
		}
		if (null != map.get("validates") && map.get("validates") instanceof List) {
			List<Map<String, Object>> validateList = (List<Map<String, Object>>) map.get("validates");
			if (null != validateList && !validateList.isEmpty()) {
				for (Map<String, Object> validate : validateList) {
					Validate v = validateService.getValidate(fieldCode + "_" + validate.get("type").toString());
					if (null == v) {
						v = new Validate();
					}
					v.setCode(fieldCode + "_" + validate.get("type").toString());
					v.setType(validate.get("type").toString());
					if (null != validate.get("param")) {
						String params = "";
						if (validate.get("param") instanceof Map) {
							Map<String, Object> paramsMap = new HashMap();
							paramsMap = (Map<String, Object>) validate.get("param");
							params = SerializeUitls.serializeAsXml(paramsMap);
						} else {
							params = validate.get("param").toString();
						}
						v.setParams(params);
					}
					v.setField(field);
					v.setModuleCode(moduleCode);
					v.setEntityCode(entityCode);
					validateService.saveValidate(v);
				}
			}
		}
		// log.info("===保存Event与Validate，花费时间:" + (System.currentTimeMillis() - dataMill000) + "ms");
		// log.info("===保存单个Field结束，花费时间:" + (System.currentTimeMillis() - sychronizeCurrent) + "ms");
	}

	/**
	 * 删除列上非指定fieldCode的字段
	 */
	public void deleteFieldByCellCodeAndExcludeFieldCode(String viewCode, Map<String, String> allFieldMap) {
		boolean isProject = ProjectFlagHolder.getInstance().getProjFlag().get() != null && ProjectFlagHolder.getInstance().getProjFlag().get();
		String tableName = isProject ? "project_field" : "ec_field";
		List<Object[]> list = fieldDao.createNativeQuery("select code, cell_code from " + tableName + " where cell_code in(select cell_code from " + tableName + " where view_code = :viewCode and valid = 1 group by cell_code having count(cell_code) > 1)")
				.setParameter("viewCode", viewCode)
				.list();
		if (list.isEmpty()) {
			return;
		}
		Set<String> deleteFieldCodeSet = new HashSet<>(list.size());
		for (Object[] columns : list) {
			String code = (String) columns[0];
			String cellCode = (String) columns[1];
			String currentFieldCode = allFieldMap.get(cellCode);
			if (code != null && cellCode != null && currentFieldCode != null && !currentFieldCode.equals(code)) {
				deleteFieldCodeSet.add(code);
			}
		}
		if(isProject){
			fieldDao.createNativeQuery("delete from project_event where field_code in (:codes)").setParameterList("codes", deleteFieldCodeSet).executeUpdate();
			fieldDao.createNativeQuery("delete from project_field where code in (:codes)").setParameterList("codes", deleteFieldCodeSet).executeUpdate();
		}else{
			fieldDao.createNativeQuery("delete from ec_event where field_code in (:codes)").setParameterList("codes", deleteFieldCodeSet).executeUpdate();
			fieldDao.createNativeQuery("delete from ec_field where code in (:codes)").setParameterList("codes", deleteFieldCodeSet).executeUpdate();
		}

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Map<String, Field> getFields(View view) {
		if (null != view) {
			Map<String, Field> fieldMap = new HashMap<String, Field>();
			List<Field> fields = this.getFields(view.getCode());
			for (Field f : fields) {
				fieldMap.put(f.getCellCode(), f);
			}
			return fieldMap;
		} else {
			return null;
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Field> findFields(String viewCode) {
		return fieldDao.findByHql("from Field where view.code=?0", viewCode);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Field> getFields(String viewCode) {
		List<Field> fieldList = new ArrayList<Field>();
//		Element element = cache.get(CACHE_FIELD_PREFIX + viewCode);
//		if (null != element) {
//			fieldList = (List<Field>) element.getObjectValue();
//		} else {
			fieldList = fieldDao.findByHql("from Field where view.code=?0", viewCode);
			for (Field f : fieldList) {
				if (!Hibernate.isInitialized(f.getEvents())) {
                    Hibernate.initialize(f.getEvents());
                }
				if (!Hibernate.isInitialized(f.getValidates())) {
                    Hibernate.initialize(f.getValidates());
                }
				if(null == f.getConfigMap()){
					Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(f.getConfig());
					f.setConfigMap(fieldMap);
				}
			}
//			cache.put(new Element(CACHE_FIELD_PREFIX + viewCode, fieldList));
//		}
		return fieldList;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional
	public void deleteFieldByViewCode(String viewCode) {
		List<Field> fields = getFields(viewCode);
		if (null != fields && !fields.isEmpty()) {
			for (Field f : fields) {
				if (f.getShowType() == FieldType.DATAGRID) {
					String config = f.getConfig();
					if (config != null && config.length() > 0) {
						Map configMap = (Map) SerializeUitls.deserialize(config);
						if (null != configMap && !configMap.isEmpty()) {
							if (null != configMap.get("field")) {
								Map<String, Object> fieldInfo = (Map<String, Object>) configMap.get("field");
								if (null != fieldInfo.get("DataGridCode") && fieldInfo.get("DataGridCode").toString().length() > 0) {
									String dgCode = fieldInfo.get("DataGridCode").toString();
									deleteFieldByDataGrid(dgCode);
								}
							}
						}
					}
				}
				deleteField(f);
			}
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Map<String, Field> getFields(DataGrid dataGrid) {
		if (null != dataGrid) {
			Map<String, Field> fieldMap = new HashMap<String, Field>();
			List<Field> fields = this.getFieldsByDataGridCode(dataGrid.getCode());
			for (Field f : fields) {
				fieldMap.put(f.getCellCode(), f);
			}
			return fieldMap;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Field> getFieldsByDataGridCode(String dataGridCode) {
		List<Field> fieldList = new ArrayList<Field>();
//		Element element = cache.get(CACHE_FIELD_PREFIX + dataGridCode);
//		if(null != element){
//			fieldList = (List<Field>) element.getObjectValue();
//		}else{
			fieldList = fieldDao.findByHql("from Field where dataGrid.code=?0", dataGridCode);
			for (Field f : fieldList) {
				Hibernate.initialize(f.getEvents());
				Hibernate.initialize(f.getValidates());
				if(null == f.getConfigMap()){
					Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(f.getConfig());
					f.setConfigMap(fieldMap);
				}
			}
//			cache.put(new Element(CACHE_FIELD_PREFIX + dataGridCode, fieldList));
//		}
		return fieldList;
	}
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Field> getFieldByFastQueryJsonCode(String fqjCode) {
		List<Field> fieldList = new ArrayList<Field>();
		fieldList = fieldDao.findByHql("from Field where fastQueryJson.code=?0", fqjCode);
		for (Field f : fieldList) {
			Hibernate.initialize(f.getEvents());
			Hibernate.initialize(f.getValidates());
			if(null == f.getConfigMap()){
				Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(f.getConfig());
				f.setConfigMap(fieldMap);
			}
		}
		return fieldList;
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Field> getFieldByAdvQueryJsonCode(String aqjCode) {
		List<Field> fieldList = new ArrayList<Field>();
		fieldList = fieldDao.findByHql("from Field where advQueryJson.code=?0", aqjCode);
		for (Field f : fieldList) {
			Hibernate.initialize(f.getEvents());
			Hibernate.initialize(f.getValidates());
			if(null == f.getConfigMap()){
				Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(f.getConfig());
				f.setConfigMap(fieldMap);
			}
		}
		return fieldList;
	}
	/**
	 * 处理field code 按新规则组织
	 * 
	 * @param moduleCode
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public void modifyFieldCode(String moduleCode) {

		viewService.backupField();

		List<View> views = null;
		if (moduleCode != null && moduleCode.length() > 0) {
			views = viewService.findViewsByModuleCode(moduleCode);
		} else {
			views = viewService.findAllViews(
					Restrictions.or(Restrictions.eq("type", ViewType.LIST), Restrictions.eq("type", ViewType.REFERENCE)),
					Restrictions.ne("showType", ShowType.LAYOUT), Restrictions.eq("valid", true));
		}
		if (views != null && !views.isEmpty()) {
			for (View view : views) {
				List<Field> fields = this.getFields(view.getCode());
				if (null != fields && !fields.isEmpty()) {
					for (Field field : fields) {
						String config = field.getConfig();
						Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(config);
						if (fieldMap != null && !fieldMap.isEmpty()) {
							Set<Event> events = field.getEvents();
							Set<Validate> validates = field.getValidates();
							if (null != events && !events.isEmpty()) {
								for (Event event : events) {
									eventService.deleteEvent(event);
								}
							}
							if (null != validates && !validates.isEmpty()) {
								for (Validate validate : validates) {
									validateService.deleteValidate(validate);
								}
							}
							fieldDao.deletePhysical(field);

							saveField(view, fieldMap, null);
						}
					}
				}
				if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW) {
					List<DataGrid> dataGrids = dataGridService.getDataGridByView(view, false);
					if (dataGrids != null && !dataGrids.isEmpty()) {
						for (DataGrid dataGrid : dataGrids) {
							List<Field> fields1 = this.getFieldsByDataGridCode(dataGrid.getCode());
							if (null != fields1 && !fields1.isEmpty()) {
								for (Field field : fields1) {
									String config = field.getConfig();
									Map<String, Object> fieldMap = (Map<String, Object>) SerializeUitls.deserialize(config);
									if (fieldMap != null && !fieldMap.isEmpty()) {
										Set<Event> events = field.getEvents();
										Set<Validate> validates = field.getValidates();
										if (null != events && !events.isEmpty()) {
											for (Event event : events) {
												eventService.deleteEvent(event);
											}
										}
										if (null != validates && !validates.isEmpty()) {
											for (Validate validate : validates) {
												validateService.deleteValidate(validate);
											}
										}
										fieldDao.deletePhysical(field);

										saveField(dataGrid, fieldMap, null);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 清除所有缓存
	 */
	@Override
	public void clearCache(){
//		if(null != cache){
//			cache.removeAll();
//		}
	}
	/**
	 * 清除指定缓存
	 * @param key
	 */
	@Override
	public void clearCache(Object key){
//		if(null != cache){
//			cache.remove(CACHE_FIELD_PREFIX + key);
//		}
	}
	
	
	/**
	 * 对所有Field进行数据处理 添加 fullPropertyCode字段
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public void dealFieldData() {
		List<Field> fields = fieldDao.loadAll();
		if (null != fields && !fields.isEmpty()) {
			for (Field field : fields) {
				if (null != field.getFullPropertyCode() && field.getFullPropertyCode().length() > 0) {
					continue;
				}
				if (null != field.getConfig() && field.getConfig().length() > 0) {
					Map<String, Object> configMap = (Map<String, Object>) SerializeUitls.deserialize(field.getConfig());
					if (null != configMap && !configMap.isEmpty()) {
						Map<String, Object> fieldMap = (Map<String, Object>) configMap.get("field");
						if (null != fieldMap.get("propertyCode")) {
							field.setFullPropertyCode(fieldMap.get("propertyCode").toString());
							this.saveField(field);
						}
					}
				}
			}
		}
	}
	
	/**
	 * 对所有Field进行数据处理 添加 fullPropertyCode字段
	 */
	@Transactional
	@Override
	public void dealFieldColumnType() {
		List<Field> fields = fieldDao.loadAll();
		if (null != fields && !fields.isEmpty()) {
			for (Field field : fields) {
				if (null != field.getProperty() && null != field.getProperty().getType()) {
					field.setColumnType(field.getProperty().getType());
				}
			}
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateFieldsByEntityCodes(String entityCodes) {
		String hql1 = "from Field f where f.showType = ?0 and f.entityCode in (:codes) and exists (select 1 from Entity e where e.code = f.entityCode and e.crossCompanyFlag = :crossCompany and e.valid = true) and f.valid = true";
		List<String> codeList = new ArrayList<String>();
		codeList.addAll(Arrays.asList(entityCodes.split(",")));
		final int perTime = 999;
		int count = codeList.size() / perTime + (codeList.size() % perTime == 0 ? 0 : 1);
		List<Field> crossComList = new ArrayList<Field>();
		for (int i = 0; i < count; i++) {
			crossComList.addAll(fieldDao.createQuery(hql1, new Object[]{FieldType.SELECTCOMP})
					.setParameterList("codes", codeList.subList(i * perTime, i * perTime + (i + 1 < count ? perTime : codeList.size() % perTime))).setParameter("crossCompany", true).list());
		}
		List<Field> unCrossComList = new ArrayList<Field>();
		for (int i = 0; i < count; i++) {
			unCrossComList.addAll(fieldDao.createQuery(hql1, new Object[]{FieldType.SELECTCOMP})
					.setParameterList("codes", codeList.subList(i * perTime, i * perTime + (i + 1 < count ? perTime : codeList.size() % perTime))).setParameter("crossCompany", false).list());
		}
		if (crossComList != null && !crossComList.isEmpty()) {
			for (Field f : crossComList) {
				if (f.getConfig() != null && f.getConfig().length() > 0) {
					Map<String, Object> cfgMap = (Map<String, Object>) ((Map<String, Object>) SerializeUitls.deserialize(f.getConfig())).get("field");
					if (cfgMap != null) {
						if (f.getRegionType() == RegionType.EDIT || f.getRegionType() == RegionType.DATAGRID) {
							cfgMap.put("isgroup", true);
						} else if (f.getRegionType() == RegionType.FASTQUERY) {
							cfgMap.put("iscrosscompany", true);
						} else {
							continue;
						}
						Map<String, Object> newMap = new HashMap<String, Object>();
						newMap.put("field", cfgMap);
						String newConfig = SerializeUitls.serializeAsXml(newMap);
						f.setConfig(newConfig);
						saveField(f);
					}
				}
			}
		}
		if (unCrossComList != null && !unCrossComList.isEmpty()) {
			for (Field f : unCrossComList) {
				if (f.getConfig() != null && f.getConfig().length() > 0) {
					Map<String, Object> cfgMap = (Map<String, Object>) ((Map<String, Object>) SerializeUitls.deserialize(f.getConfig())).get("field");
					if (cfgMap != null) {
						if (f.getRegionType() == RegionType.EDIT || f.getRegionType() == RegionType.DATAGRID) {
							cfgMap.put("isgroup", false);
						} else if (f.getRegionType() == RegionType.FASTQUERY) {
							cfgMap.put("iscrosscompany", false);
						} else {
							continue;
						}
						Map<String, Object> newMap = new HashMap<String, Object>();
						newMap.put("field", cfgMap);
						String newConfig = SerializeUitls.serializeAsXml(newMap);
						f.setConfig(newConfig);
						saveField(f);
					}
				}
			}
		}
		//布局视图
		String hql2 = "from ExtraView ev where (ev.view.showType = ?0 or ev.view.showType = ?1) and ev.view.valid = true and ev.view.entity.crossCompanyFlag = ?2 and ev.view.entity.code in (:codes) and ev.view.entity.valid = true";
		List<ExtraView> clayList = new ArrayList<ExtraView>();
		for (int i = 0; i < count; i++) {
			clayList.addAll(fieldDao.createQuery(hql2, new Object[] { ShowType.LAYOUT, ShowType.LAYOUT2, true })
					.setParameterList("codes", codeList.subList(i * perTime, i * perTime + (i + 1 < count ? perTime : codeList.size() % perTime))).list());
		}
		List<ExtraView> ulayList = new ArrayList<ExtraView>();
		for (int i = 0; i < count; i++) {
			ulayList.addAll(fieldDao.createQuery(hql2, new Object[] { ShowType.LAYOUT, ShowType.LAYOUT2, false })
					.setParameterList("codes", codeList.subList(i * perTime, i * perTime + (i + 1 < count ? perTime : codeList.size() % perTime))).list());
		}
		if (clayList != null && clayList.size() > 0) {
			for (ExtraView ev : clayList) {
				if (ev.getConfig() != null && ev.getConfig().length() > 0) {
					Map<String, Object> layoutMap = (Map<String, Object>) ((Map<String, Object>) SerializeUitls.deserialize(ev.getConfig())).get("layout");
					for (Entry<String, Object> entry : layoutMap.entrySet()) {
						Map<String, Object> regionMap = (Map<String, Object>) entry.getValue();
						regionMap.put("tree_crossCompanyFlag", "yes");
					}
					Map<String, Object> newMap = new HashMap<String, Object>();
					newMap.put("layout", layoutMap);
					String newConfig = SerializeUitls.serializeAsXml(newMap);
					ev.setConfig(newConfig);
					viewDao.saveExtraView(ev);
				}
			}
		}
		if (ulayList != null && ulayList.size() > 0) {
			for (ExtraView ev : ulayList) {
				if (ev.getConfig() != null && ev.getConfig().length() > 0) {
					Map<String, Object> layoutMap = (Map<String, Object>) ((Map<String, Object>) SerializeUitls.deserialize(ev.getConfig())).get("layout");
					for (Entry<String, Object> entry : layoutMap.entrySet()) {
						Map<String, Object> regionMap = (Map<String, Object>) entry.getValue();
						regionMap.put("tree_crossCompanyFlag", "none");
					}
					Map<String, Object> newMap = new HashMap<String, Object>();
					newMap.put("layout", layoutMap);
					String newConfig = SerializeUitls.serializeAsXml(newMap);
					ev.setConfig(newConfig);
					viewDao.saveExtraView(ev);
				}
			}
		}
		// 清空缓存
//		List<Ehcache> caches = cacheAdmin.getAllCaches();
//		if (caches != null && caches.size() > 0) {
//			for (int i = 0; i < caches.size(); i++) {
//				Ehcache cache = caches.get(i);
//				cache.clearStatistics();
//				cache.removeAll();
//			}
//		}
	}
	
	/**
	 * 根据条件查询Field
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Field> findFields(Criterion... criterions) {
		return fieldDao.findByCriteria(criterions);
	}
	
	@Override
	public void saveSelectionRange(SelectionRange range) {
		selectionRangeDao.save(range);
	}
	
	@Override
	public void deleteSelectionRange(Long id){
		selectionRangeDao.delete(id);
	}

	@Override
	public SelectionRange getSelectionRangeById(Long id) {
		List<SelectionRange> result = selectionRangeDao.findByHql("From SelectionRange where valid = ? and id = ?", true, id);
		if(result.size()>0){
			return result.get(0);
		}
		return null;
	}
	
	@Override
	public List<SelectionRange> findSelectionRanges(Criterion... criterions) {
		return selectionRangeDao.findByCriteria(criterions);
	}
	
	@Override
	public List<SelectionRange> getSelectionRangeByFieldCode(String fieldCode){
		List<SelectionRange> result = selectionRangeDao.findByHql("From SelectionRange where valid = ?0 and fieldCode = ?1", true, fieldCode);
		return result;
	}

	@Override
	public void deleteSelectionRangeByField(Field field) {
		selectionRangeDao.deleteAll(getSelectionRangeByFieldCode(field.getCode()));
	}

	@Override
	public Field findFieldByCellCode(String cellCode, View view) {
		return fieldDao.findEntityByCriteria(Restrictions.eq("cellCode", cellCode), Restrictions.eq("view", view));
	}
	
}
