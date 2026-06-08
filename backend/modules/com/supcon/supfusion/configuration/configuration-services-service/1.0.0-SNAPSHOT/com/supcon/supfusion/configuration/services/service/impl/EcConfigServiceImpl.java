/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.utils.PropertyHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.FieldType;
import com.supcon.supfusion.configuration.services.enums.ShowType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.utils.EcExtraViewIntegrationUtils;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.configuration.services.dao.DataGridDaoImpl;
import com.supcon.supfusion.configuration.services.dao.ModuleDaoImpl;
import com.supcon.supfusion.configuration.services.enums.EcEntityEnum;
import com.supcon.supfusion.configuration.services.service.*;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 
 * 获取完整的视图配置信息类
 * 
 * @author zhuyuyin
 * @version $Id$
 */
@Slf4j
@ServiceApiService("ec_ecConfigService")
@Transactional
public class EcConfigServiceImpl implements EcConfigService {

	@Autowired
	private FieldService fieldService;
	@Autowired
	private EventService eventService;
	@Autowired
	private ButtonService buttonService;
	@Autowired
	private DataGridService dataGridService;
	@Autowired
	private ViewService viewService;
	@Autowired
	private ModelService modelService;

	@Autowired
	private DataGridDaoImpl dataGridDao;
	@Autowired
	private ModuleDaoImpl moduleDao;

	/**
	 * 获取完整的视图配置信息
	 * 
	 * @param object
	 * @return
	 */
	// @Transactional(readOnly=true)
	@Override
	public String getEcFullConfig(Object object) {
		Map<String, List<?>> infoMap = getFieldInfoMap(object);
		String evConfig = new EcExtraViewIntegrationUtils().ecExtraViewIntegrationBuild(object, infoMap);
		return evConfig;
	}
	
	/**
	 * 主要内容拷贝 {@link #getEcFullConfig(Object)}，是为了优化准备数据过程所提供的方法。所有的计算均依赖于已经准备好的数据，不再进行数据库查询。
	 * <p>
	 * FIXME 如果测试通过，原本的方法需要删除或整合。
	 * 
	 * @param object
	 * @param allEvents
	 * @param allFields
	 * @param allButtons
	 * @return
	 */
	public static String getEcFullConfig(Object object, List<Event> allEvents, List<Field> allFields, List<Button> allButtons) {
		Map<String, List<?>> infoMap = getFieldInfoMap(object, allEvents, allFields, allButtons);
		String evConfig = new EcExtraViewIntegrationUtils().ecExtraViewIntegrationBuild(object, infoMap);
		return evConfig;
	}

	/**
	 * 根据DataGrid获取关联视图的字段属性Map
	 * 
	 * @param dataGrid
	 * @return
	 */
	// @Transactional(readOnly=true)
	@Override
	public String getViewFieldConfigByDataGrid(DataGrid dataGrid) {
		View view = dataGrid.getView();
		if (view != null) {
			return getFieldsConfig(view);
		}

		return null;
	}

	/**
	 * 根据视图获取关联DataGrid的字段属性Map
	 * 
	 * @param view
	 * @return
	 */
	// @Transactional(readOnly=true)
	@Override
	public Map<String, String> getDataGridFieldConfigByView(View view) {
		List<DataGrid> dataGrids = dataGridService.getDataGridByView(view, false);
		if (dataGrids != null && !dataGrids.isEmpty()) {
			Map<String, String> map = new HashMap<String, String>();
			for (DataGrid dataGrid : dataGrids) {
				String config = getFieldsConfig(dataGrid);
				if (config != null && config.length() > 0) {
					map.put(dataGrid.getCode(), config);
				}
			}
			return map;
		}
		return null;
	}

	/**
	 * 获取视图字段属性信息
	 * 
	 * @param object
	 * @return xml String
	 */
	// @Transactional(readOnly=true)
	@Override
	public String getFieldsConfig(Object object) {
		Map<String, List<?>> infoMap = getFieldInfoMap(object);
		if (infoMap != null && !infoMap.isEmpty()) {
			Map<String, List<?>> attMap = new EcExtraViewIntegrationUtils().getFieldListMap(infoMap);
			if (attMap != null && !attMap.isEmpty()) {
				return SerializeUitls.serializeAsXml(attMap);
			}
		}
		return null;
	}
	
	/**
	 * 本方法主要逻辑从 {@link #getFieldInfoMap(Object)} 拷贝，但所有相关数据均从传入的数组中获取，不再进行数据库查询。
	 * <p>
	 * FIXME 待性能调整测试完毕后，原方法需要删除
	 * 
	 * @param object
	 * @param allEvents
	 * @param allFields
	 * @param allButtons
	 * @return
	 */
	private static Map<String, List<?>> getFieldInfoMap(Object object, List<Event> allEvents, List<Field> allFields, List<Button> allButtons) {
		Map<String, List<?>> infoMap = new HashMap<String, List<?>>();
		String config = "";
		View view = null;
		DataGrid dataGrid = null;
		FastQueryJson fastQueryJson = null;
		AdvQueryJson advQueryJson = null;
		Map<String, Object> configMap = new HashMap<String, Object>();
		List<Event> events = null;
		if (object instanceof View) {
			view = (View) object;
			ExtraView ev = view.getExtraView();
			if (null == ev) {
				ev = new ExtraView();
			}
			if (null != view.getIsShadow() && view.getIsShadow() && null != view.getShadowView()) {
				if (null != view.getShadowView().getExtraView()) {
					ev.setConfig(view.getShadowView().getExtraView().getConfig());
					ev.setConfigMap(view.getShadowView().getExtraView().getConfigMap());
				}
				events = view.getShadowView().getEvents();
			} else if (null != view.getEvents() && !view.getEvents().isEmpty()) {
				events = view.getEvents();
			}
			if (null != ev && null != ev.getConfig() && ev.getConfig().length() > 0) {
				config = ev.getConfig();
			}
			if (null != ev && null != ev.getConfigMap()) {
				configMap = ev.getConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}

		} else if (object instanceof DataGrid) {
			dataGrid = (DataGrid) object;
			if (null != dataGrid.getConfig() && dataGrid.getConfig().length() > 0) {
				config = dataGrid.getConfig();
			}
			if (null != dataGrid.getConfigMap()) {
				configMap = dataGrid.getConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}
			if (null != dataGrid.getEvents()) {
				events = dataGrid.getEvents();
			}
		} else if (object instanceof FastQueryJson) {
			fastQueryJson = (FastQueryJson) object;
			if (null != fastQueryJson.getQueryConfig() && fastQueryJson.getQueryConfig().length() > 0) {
				config = fastQueryJson.getQueryConfig();
			}
			if (null != fastQueryJson.getQueryConfigMap()) {
				configMap = fastQueryJson.getQueryConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}
			if (null != fastQueryJson.getEvents()) {
				events = fastQueryJson.getEvents();
			}
		} else if (object instanceof AdvQueryJson) {
			advQueryJson = (AdvQueryJson) object;
			if (null != advQueryJson.getQueryConfig() && advQueryJson.getQueryConfig().length() > 0) {
				config = advQueryJson.getQueryConfig();
			}
			if (null != advQueryJson.getQueryConfigMap()) {
				configMap = advQueryJson.getQueryConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}
			if (null != advQueryJson.getEvents()) {
				events = advQueryJson.getEvents();
			}
		} 

		if (configMap != null && !configMap.isEmpty()) {
			Map layout = (Map) configMap.get("layout");
			if (layout != null && !layout.isEmpty()) {
				if (layout.get("layoutCode") != null && layout.get("layoutCode").toString().length() > 0) {
					String layoutCode = layout.get("layoutCode").toString();
					if (null == events || events.isEmpty()) {
						events = new ArrayList<>();
						for(Event event : allEvents) {
							if(layoutCode.equals(event.getLayoutCode())) {
								events.add(event);
							}
						}
					}
					if (events != null && !events.isEmpty()) {
						infoMap.put("events", events);
					}
				}
			}
		}

		List<Field> fields = null;

		List<Button> buttons = null;

		if (object instanceof View) {
			String code = view.getCode();
			if (null != view.getIsShadow() && view.getIsShadow() && null != view.getShadowView()) {
				code = view.getShadowView().getCode();
			}
			if (null != view.getFields() && !view.getFields().isEmpty()) {
				fields = view.getFields();
			} else {
				fields = new ArrayList<>();
				for(Field field : allFields) { // allFields 里包含了所有View和DataGrid的Field，根据Field所归属的模型不同，相关字段也记录的不同内容。
					View fieldView = field.getView();
					if(null != fieldView && code.equals(fieldView.getCode())) {
						fields.add(field);
					}
				}
			}
			// view = viewService.getView(view.getCode(), true);
			if (view.getDataGrids() != null && !view.getDataGrids().isEmpty()) {
				List<Field> dgFileds = new ArrayList<Field>();
				for (Field field : fields) {
					if (field.getShowType() == FieldType.DATAGRID) {
						dgFileds.add(field);
					}
				}
				List<Field> delFields = new ArrayList<Field>();
				if (dgFileds != null && !dgFileds.isEmpty()) {
					for (Field field : dgFileds) {
						if (field.getConfig() != null && field.getConfig().length() > 0) {
							Map fieldMap = null;
							if (null != field.getConfigMap()) {
								fieldMap = (Map) field.getConfigMap().get("field");
							} else {
								fieldMap = (Map) ((Map) SerializeUitls.deserialize(field.getConfig())).get("field");
							}
							if (fieldMap != null && !fieldMap.isEmpty()) {
								if (fieldMap.get("DataGridCode") != null) {
									String dgCode = fieldMap.get("DataGridCode").toString();
									for (DataGrid grid : view.getDataGrids()) {
										if (grid.getCode().equals(dgCode) && !grid.isValid()) {
											delFields.add(field);
											break;
										}
									}
								}
							}
						}
					}
				}
				if (delFields != null && !delFields.isEmpty()) {
					fields.removeAll(delFields);
					//FIXME 这里的逻辑应该在外部进行，比如与SQL生成逻辑放在一起
//					for (Field field : delFields) {
//						fieldService.deleteField(field);
//					}
				}
			}
			if (null != view.getButtons() && !view.getButtons().isEmpty()) {
				buttons = view.getButtons();
			} else {
				buttons = new ArrayList<>();
				for(Button button : allButtons) {
					View btnView = button.getView();
					if(null != btnView && code.equals(btnView.getCode())) {
						buttons.add(button);
						if(null == button.getEvents() || button.getEvents().isEmpty()) {
							Set<Event> btnEvents = new HashSet<>();
							for(Event event : allEvents) {
								Button eventBtn = event.getButton();
								if(null != eventBtn && button.getCode().equals(eventBtn.getCode())) {
									btnEvents.add(event);
								}
							}
							button.setEvents(btnEvents);
						}
					}
				}
			}
		} else if (object instanceof DataGrid) {
			dataGrid = (DataGrid) object;
			if (null != dataGrid.getFields() && !dataGrid.getFields().isEmpty()) {
				fields = dataGrid.getFields();
			} else {
				fields = new ArrayList<>();
				for(Field field : allFields) {
					if(null != field.getDataGrid() && field.getDataGrid().getCode().equals(dataGrid.getCode())) {
						fields.add(field);
					}
				}
			}
			if (null != dataGrid.getButtons() && !dataGrid.getButtons().isEmpty()) {
				buttons = dataGrid.getButtons();
			} else {
				buttons = new ArrayList<>();
				for(Button button : allButtons) {
					if(null != button.getDataGrid() && button.getDataGrid().getCode().equals(dataGrid.getCode())) {
						buttons.add(button);
					}
				}
			}
		}
		if (fields != null && !fields.isEmpty()) {
			infoMap.put("fields", fields);
		}
		if (buttons != null && !buttons.isEmpty()) {
			infoMap.put("buttons", buttons);
		}
		return infoMap;
	
	}

	/**
	 * @param object
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, List<?>> getFieldInfoMap(Object object) {
		Map<String, List<?>> infoMap = new HashMap<String, List<?>>();
		String config = "";
		View view = null;
		DataGrid dataGrid = null;
		FastQueryJson fastQueryJson = null;
		AdvQueryJson advQueryJson = null;
		Map<String, Object> configMap = new HashMap<String, Object>();
		List<Event> events = null;
		if (object instanceof View) {
			view = (View) object;
			ExtraView ev = view.getExtraView();
			if (null == ev) {
				ev = new ExtraView();
			}
			if (null != view.getIsShadow() && view.getIsShadow() && null != view.getShadowView()) {
				if (null != view.getShadowView().getExtraView()) {
					ev.setConfig(view.getShadowView().getExtraView().getConfig());
					ev.setConfigMap(view.getShadowView().getExtraView().getConfigMap());
				}
				events = view.getShadowView().getEvents();
			} else if (null != view.getEvents() && !view.getEvents().isEmpty()) {
				events = view.getEvents();
			}
			if (null != ev && null != ev.getConfig() && ev.getConfig().length() > 0) {
				config = ev.getConfig();
			}
			if (null != ev && null != ev.getConfigMap()) {
				configMap = ev.getConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}

		} else if (object instanceof DataGrid) {
			dataGrid = (DataGrid) object;
			if (null != dataGrid.getConfig() && dataGrid.getConfig().length() > 0) {
				config = dataGrid.getConfig();
			}
			if (null != dataGrid.getConfigMap()) {
				configMap = dataGrid.getConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}
			if (null != dataGrid.getEvents()) {
				events = dataGrid.getEvents();
			}
		} else if (object instanceof FastQueryJson) {
			fastQueryJson = (FastQueryJson) object;
			if (null != fastQueryJson.getQueryConfig() && fastQueryJson.getQueryConfig().length() > 0) {
				config = fastQueryJson.getQueryConfig();
			}
			if (null != fastQueryJson.getQueryConfigMap()) {
				configMap = fastQueryJson.getQueryConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}
			if (null != fastQueryJson.getEvents()) {
				events = fastQueryJson.getEvents();
			}
		} else if (object instanceof AdvQueryJson) {
			advQueryJson = (AdvQueryJson) object;
			if (null != advQueryJson.getQueryConfig() && advQueryJson.getQueryConfig().length() > 0) {
				config = advQueryJson.getQueryConfig();
			}
			if (null != advQueryJson.getQueryConfigMap()) {
				configMap = advQueryJson.getQueryConfigMap();
			} else {
				configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			}
			if (null != advQueryJson.getEvents()) {
				events = advQueryJson.getEvents();
			}
		} 

		if (configMap != null && !configMap.isEmpty()) {
			Map layout = (Map) configMap.get("layout");
			if (layout != null && !layout.isEmpty()) {
				if (layout.get("layoutCode") != null && layout.get("layoutCode").toString().length() > 0) {
					String layoutCode = layout.get("layoutCode").toString();
					if (null == events || events.isEmpty()) {
						events = eventService.getEventsByLayoutCode(layoutCode);
					}
					if (events != null && !events.isEmpty()) {
						infoMap.put("events", events);
					}
				}
			}
		}

		List<Field> fields = null;

		List<Button> buttons = null;

		if (object instanceof View) {
			String code = view.getCode();
			if (null != view.getIsShadow() && view.getIsShadow() && null != view.getShadowView()) {
				code = view.getShadowView().getCode();
			}
			if (null != view.getFields() && !view.getFields().isEmpty()) {
				fields = view.getFields();
			} else {
				fields = fieldService.getFields(code);
			}
			// view = viewService.getView(view.getCode(), true);
			if (view.getDataGrids() != null && !view.getDataGrids().isEmpty()) {
				List<Field> dgFileds = new ArrayList<Field>();
				for (Field field : fields) {
					if (field.getShowType() == FieldType.DATAGRID) {
						dgFileds.add(field);
					}
				}
				List<Field> delFields = new ArrayList<Field>();
				if (dgFileds != null && !dgFileds.isEmpty()) {
					for (Field field : dgFileds) {
						if (field.getConfig() != null && field.getConfig().length() > 0) {
							Map fieldMap = null;
							if (null != field.getConfigMap()) {
								fieldMap = (Map) field.getConfigMap().get("field");
							} else {
								fieldMap = (Map) ((Map) SerializeUitls.deserialize(field.getConfig())).get("field");
							}
							if (fieldMap != null && !fieldMap.isEmpty()) {
								if (fieldMap.get("DataGridCode") != null) {
									String dgCode = fieldMap.get("DataGridCode").toString();
									for (DataGrid grid : view.getDataGrids()) {
										if (grid.getCode().equals(dgCode) && !grid.isValid()) {
											delFields.add(field);
											break;
										}
									}
								}
							}
						}
					}
				}
				if (delFields != null && !delFields.isEmpty()) {
					fields.removeAll(delFields);
					for (Field field : delFields) {
						fieldService.deleteField(field);//FIXME 这里的代码不合适，应该放到外部进行，而不是在数据准备阶段。
					}
				}
			}
			if (null != view.getButtons() && !view.getButtons().isEmpty()) {
				buttons = view.getButtons();
			} else {
				buttons = buttonService.getButtons(code);
			}
		} else if (object instanceof DataGrid) {
			if (null != dataGrid.getFields() && !dataGrid.getFields().isEmpty()) {
				fields = dataGrid.getFields();
			} else {
				fields = fieldService.getFieldsByDataGridCode(dataGrid.getCode());
			}
			if (null != dataGrid.getButtons() && !dataGrid.getButtons().isEmpty()) {
				buttons = dataGrid.getButtons();
			} else {
				buttons = buttonService.getButtonsByDataGridCode(dataGrid.getCode());
			}
		} else if(object instanceof FastQueryJson) {
			fields = fieldService.getFieldByFastQueryJsonCode(fastQueryJson.getCode());
		} else if (object instanceof AdvQueryJson) {
			fields = fieldService.getFieldByAdvQueryJsonCode(advQueryJson.getCode());
		}
		if (fields != null && !fields.isEmpty()) {
			infoMap.put("fields", fields);
		}
		if (buttons != null && !buttons.isEmpty()) {
			infoMap.put("buttons", buttons);
		}
		return infoMap;
	}

	/**
	 * 更改视图或DataGrid的配置信息 层级结构更改并抽取字段属性信息
	 * 
	 * @param moduleCode
	 *            模块code
	 */
	@Override
	@Transactional(timeout = -1)
	public void modifyConfiguration(String moduleCode) {

		// 处理前备份涉及的表
		// viewService.backupViewConfig();

		// 将模块下的DataGrid全部重置为valid=true
		List<DataGrid> dataGridList = new ArrayList<DataGrid>();
		if (moduleCode != null && moduleCode.length() > 0) {
			dataGridList = dataGridDao.findByCriteria(Restrictions.eq("valid", false), Restrictions.like("code", moduleCode, MatchMode.START));
		} else {
			dataGridList = dataGridDao.findByCriteria(Restrictions.eq("valid", false));
		}
		if (dataGridList != null && !dataGridList.isEmpty()) {
			for (DataGrid dataGrid : dataGridList) {
				dataGrid.setValid(true);
				dataGridService.save(dataGrid);
			}
		}
		List<View> views = null;
		if (moduleCode != null && moduleCode.length() > 0) {
			views = viewService.findViewsByModuleCode(moduleCode);
		} else {
			views = viewService.findAllViews(Restrictions.eq("valid", true));
		}
		if (views != null && !views.isEmpty()) {
			for (View view : views) {
				if (view.getShowType() != ShowType.LAYOUT) {
					ExtraView ev = view.getExtraView();
					if (ev != null && ev.getConfig() != null && ev.getConfig().length() > 0) {
						String viewConfig = new EcExtraViewIntegrationUtils().modifyConfigStructure(ev.getConfig());
						if (viewConfig != null && viewConfig.length() > 0) {
							Map<String, Object> configMap = new EcExtraViewIntegrationUtils().ecSplitConfig(viewConfig);
							if (configMap.get("config") != null) {
								String config = configMap.get("config").toString();
								ev.setConfig(config);
								viewService.saveExtraView(ev, null);
							}
							if (configMap.get("fieldConfig") != null) {
								String fieldConfig = configMap.get("fieldConfig").toString();
								if (view.getType() != ViewType.MNECODE) {
									buttonService.saveButton(view, fieldConfig, null);
								}
								fieldService.saveFields(view, fieldConfig, null, null, null);
								if (view.getType() != ViewType.MNECODE) {
									eventService.saveEvent(view, fieldConfig);
								}
							}
						}

						if (view.getType() == ViewType.EDIT || view.getType() == ViewType.VIEW) {
							List<DataGrid> dataGrids = dataGridService.getDataGridByView(view, false);
							if (dataGrids != null && !dataGrids.isEmpty()) {
								for (DataGrid dataGrid : dataGrids) {
									if (dataGrid.getConfig() != null && dataGrid.getConfig().length() > 0) {
										String dgConfig = new EcExtraViewIntegrationUtils().modifyConfigStructure(dataGrid.getConfig());
										if (dgConfig != null && dgConfig.length() > 0) {
											Map<String, Object> dgConfigMap = new EcExtraViewIntegrationUtils().ecSplitConfig(dgConfig);
											if (dgConfigMap.get("config") != null) {
												dataGrid.setConfig(dgConfigMap.get("config").toString());
												dataGridService.save(dataGrid);
											}

											if (dgConfigMap.get("fieldConfig") != null) {
												String fieldConfig = dgConfigMap.get("fieldConfig").toString();
												fieldService.saveFields(dataGrid, fieldConfig, null, null, null);
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
		// dealHrAndPartCss();
		buttonService.addOperateType(moduleCode);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	@Override
	public void dealFieldByFastQueryDateAndButton(String moduleCode) {

		viewService.backupField();

		List<View> views = null;
		if (moduleCode != null && moduleCode.length() > 0) {
			views = viewService.findViewsByModuleCode(moduleCode);
		} else {
			views = viewService.findAllViews(Restrictions.or(Restrictions.eq("type", ViewType.LIST), Restrictions.eq("type", ViewType.REFERENCE)),
					Restrictions.ne("showType", ShowType.LAYOUT), Restrictions.eq("valid", true));
		}
		if (views != null && !views.isEmpty()) {
			for (View view : views) {
				if ((view.getType() == ViewType.LIST || view.getType() == ViewType.REFERENCE) && view.getShowType() != ShowType.LAYOUT) {
					ExtraView ev = view.getExtraView();
					String fullConfig = getEcFullConfig(view);
					Map configMap = (Map) SerializeUitls.deserialize(fullConfig);
					if (null != configMap && !configMap.isEmpty()) {
						Map layoutMap = (Map) configMap.get("layout");
						if (layoutMap != null && !layoutMap.isEmpty()) {
							List<Map> sectionList = (List<Map>) layoutMap.get("sections");
							if (sectionList != null && !sectionList.isEmpty()) {
								for (Map section : sectionList) {
									if (section.get("regionType") != null && "FASTQUERY".equals(section.get("regionType").toString())) {
										List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
										if (cells != null && !cells.isEmpty()) {
											for (Iterator<Map<String, Object>> it = cells.iterator(); it.hasNext();) {
												Map<String, Object> cell = it.next();
												if (null != cell.get("element")) {
													Map<String, Object> elementMap = (Map<String, Object>) cell.get("element");
													Property p = null;
													if (elementMap.get("propertyCode") != null && elementMap.get("propertyCode").toString().length() > 0) {
														String propertyCode = elementMap.get("propertyCode").toString();
														String[] codeStr = propertyCode.split("\\|\\|");
														p = modelService.getProperty(codeStr[codeStr.length - 1]);
													}
													if (null != elementMap.get("showType")
															&& ("LABEL".equals(elementMap.get("showType").toString()) || "BUTTON"
																	.equals(elementMap.get("showType").toString()))) {
														it.remove();
														continue;
													} else if (null != elementMap.get("columnType")
															&& ("DATE".equals(elementMap.get("columnType").toString())
																	|| "DATETIME".equals(elementMap.get("columnType").toString())
																	|| "INTEGER".equals(elementMap.get("columnType").toString())
																	|| "LONG".equals(elementMap.get("columnType").toString())
																	|| "DECIMAL".equals(elementMap.get("columnType").toString()) || "MONEY".equals(elementMap
																	.get("columnType").toString()))) {
														if (null != elementMap.get("name") && elementMap.get("name").toString().endsWith("_end")) {
															it.remove();
															continue;
														}
														if (null != elementMap.get("name") && elementMap.get("name").toString().endsWith("_start")) {
															String name = elementMap.get("name").toString();
															int index = name.lastIndexOf("_start");
															String newName = name.substring(0, index);
															elementMap.put("name", newName);
															elementMap.put("key", newName);
															elementMap.put("exp", "equal");
															if (null != p && null != p.getDisplayName() && p.getDisplayName().length() > 0) {
																elementMap.put("namekey", p.getDisplayName());
															}
														}
													} else {
														if (null != p && null != p.getDisplayName() && p.getDisplayName().length() > 0) {
															elementMap.put("namekey", p.getDisplayName());
														}
													}
												}
											}
										}
									}
								}
							}
						}

						Map<String, Object> splitMap = new EcExtraViewIntegrationUtils().ecSplitConfig(SerializeUitls.serializeAsXml(configMap));
						fieldService.deleteFieldByViewCode(view.getCode());
						if (splitMap.get("config") != null) {
							String config = splitMap.get("config").toString();
							ev.setConfig(config);
							viewService.saveExtraView(ev, null);
						}
						if (splitMap.get("fieldConfig") != null) {
							String fieldConfig = splitMap.get("fieldConfig").toString();
							if (view.getType() != ViewType.MNECODE) {
								buttonService.saveButton(view, fieldConfig, null);
							}
							fieldService.saveFields(view, fieldConfig, null, null, null);
							if (view.getType() != ViewType.MNECODE) {
								eventService.saveEvent(view, fieldConfig);
							}
						}
					}
				}
			}
		}

	}

	/**
	 * 处理党群与人力模块可空非空样式
	 */
	@Transactional(timeout = -1)
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void dealHrAndPartCss() {
		List<View> views = viewService.findAllViews(Restrictions.eq("valid", true), Restrictions.eq("type", ViewType.EDIT),
				Restrictions.or(Restrictions.like("code", "hr_1.0", MatchMode.START), Restrictions.like("code", "part_1.0", MatchMode.START)));
		if (views != null && !views.isEmpty()) {
			for (View view : views) {
				if (view.getExtraView() == null || view.getExtraView().getConfig() == null || view.getExtraView().getConfig().isEmpty()) {
                    continue;
                }
				ExtraView ev = view.getExtraView();
				String config = getEcFullConfig(view);
				if (config != null && config.length() > 0) {
					Map<String, Object> configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
					if (configMap != null && !configMap.isEmpty()) {
						Map<String, Object> layout = (Map<String, Object>) configMap.get("layout");
						if (layout != null && !layout.isEmpty()) {
							List<Map> tabs = (List<Map>) layout.get("tabs");
							if (null != tabs && !tabs.isEmpty()) {
								for (Map tab : tabs) {
									List<Map> sections = (List<Map>) tab.get("sections"); // 页签中的section
									if (sections != null && !sections.isEmpty()) {
										for (Map section : sections) {
											if (section.get("regionType") != null) {
												List<Map> cells = (List<Map>) section.get("cells");
												if (cells != null && !cells.isEmpty()) {
													for (int i = 0; i < cells.size(); i++) {
														Map cell = cells.get(i);
														Map<String, Object> element = (Map<String, Object>) cell.get("element");// 获取element
														if (null != element && !element.isEmpty()) {
															Object showType = element.get("showType");
															if (showType != null && !"LABEL".equals(showType.toString())) {
																Object nullable = element.get("nullable");
																if (nullable != null && "false".equalsIgnoreCase(nullable.toString())) {
																	// Object rowspan = cell.get("rowspan");
																	if (i > 0) {
																		Map cellBefore = cells.get(i - 1);
																		Map<String, Object> elementBefore = (Map<String, Object>) cellBefore.get("element");// 获取element
																		if (null != elementBefore && !elementBefore.isEmpty()) {
																			Object showTypeBefore = elementBefore.get("showType");
																			if (showTypeBefore != null && "LABEL".equals(showTypeBefore.toString())) {
																				elementBefore.put("nullable", nullable.toString());// 可空非空保持一致
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
									}
								}
							}
						}

						// 保存
						Map<String, Object> map = new EcExtraViewIntegrationUtils().ecSplitConfig(SerializeUitls.serializeAsXml(configMap));
						if (map != null && !map.isEmpty()) {
							if (map.get("config") != null) {
								String evconfig = map.get("config").toString();
								ev.setConfig(evconfig);
								viewService.saveExtraView(ev, null);
							}
							if (map.get("fieldConfig") != null) {
								String fieldConfig = map.get("fieldConfig").toString();
								fieldService.saveFields(view, fieldConfig, null, null, null);
								/*
								 * if (view.getType() != ViewType.MNECODE) {
								 * eventService.saveEvent(view, fieldConfig);
								 * }
								 */
							}
						}
					}
				}
			}
		}
	}

	@Autowired
	public void setFieldService(FieldService fieldService) {
		this.fieldService = fieldService;
	}

	@Autowired
	public void setEventService(EventService eventService) {
		this.eventService = eventService;
	}

	@Autowired
	public void setButtonService(ButtonService buttonService) {
		this.buttonService = buttonService;
	}

	@Autowired
	public void setDataGridService(DataGridService dataGridService) {
		this.dataGridService = dataGridService;
	}

	@Autowired
	public void setViewService(ViewService viewService) {
		this.viewService = viewService;
	}

	/**
	 * @param modelService
	 *            the modelService to set
	 */
	@Autowired
	public void setModelService(ModelService modelService) {
		this.modelService = modelService;
	}

	@Override
	public void dealEcEnv() {
		for (EcEntityEnum enumItem : EcEntityEnum.values()) {
			moduleDao.bulkExecute("update " + enumItem.getClazz().getName() + " set ecEnv = ? where ecEnv is null", PropertyHolder.getEcEnv());
		}
	}

}
