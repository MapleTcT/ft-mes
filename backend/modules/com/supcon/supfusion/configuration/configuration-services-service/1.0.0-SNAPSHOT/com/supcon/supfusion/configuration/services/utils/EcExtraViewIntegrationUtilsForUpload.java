package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.entity.EcEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * 视图布局配置信息与字段信息整合公共类
 * 
 * @author zhuyuyin
 * @version $Id$
 */
public class EcExtraViewIntegrationUtilsForUpload {

	private static final String[] PAGE_EVENTS = { "onload", "onsave", "beforeSave", "afterSave", "beforeSubmit", "afterSubmit" };
	private static final String[] FILTER_EVENTS = {"onload", "onsave"};
	//field 的初始化值，如果有新增，新添加
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EcExtraViewIntegrationUtilsForUpload.class);

	private LinkedList<Map<String, Object>> eventsList = null;
	private LinkedList<Map<String, Object>> buttonsList = null;
	private LinkedList<Map<String, Object>> validateList = null;
	private LinkedList<TreeMap<String, Object>> fieldsList = null;

	private static String generateFieldCode(String pElementCode, Map<String, Object> fieldMap) {
		String fieldCode = pElementCode + "_" + fieldMap.get("regionType").toString() + "_";
		if (null != fieldMap.get("propertyCode") && fieldMap.get("propertyCode").toString().length() > 0) {
			if (null != fieldMap.get("showType") && fieldMap.get("showType").toString().equals("LABEL")) {
				fieldCode += "LABEL_";
			} else {
				fieldCode += "OTHER_";
			}
			String propertyCode = fieldMap.get("propertyCode").toString();

			fieldCode += propertyCode.replace("||", "_");

			if ((fieldMap.get("regionType").toString().equals("LISTPT") || fieldMap.get("regionType").toString().equals("DATAGRID"))
					&& null != fieldMap.get("none")) {
				fieldCode += "_none";
			}
			if (fieldMap.get("regionType").toString().equals("FASTQUERY")) {
				String key = "";
				if (null != fieldMap.get("key") && fieldMap.get("key").toString().length() > 0) {
					key = fieldMap.get("key").toString();
				} else if (null != fieldMap.get("name") && fieldMap.get("name").toString().length() > 0) {
					key = fieldMap.get("name").toString();
				}
				if (key != null && key.length() > 0) {
					if (key.indexOf("_start") > -1) {
						fieldCode += "_start";
					} else if (key.indexOf("_end") > -1) {
						fieldCode += "_end";
					}
				}
			}
		} else {
			if (null != fieldMap.get("DataGridCode") && fieldMap.get("DataGridCode").toString().length() > 0) {
				String dgCode = fieldMap.get("DataGridCode").toString();
				// long dataMill = System.currentTimeMillis();
				fieldCode += dgCode.replaceAll("\\.", "_");
			} else if (fieldMap.get("assoFlag") != null && fieldMap.get("assoFlag").toString().equalsIgnoreCase("true")) {
				fieldCode = (String) fieldMap.get("code");
			} else {
				if (null != fieldMap.get("key") && fieldMap.get("key").toString().length() > 0) {
					String key = fieldMap.get("key").toString();
					fieldCode += key.replaceAll("\\.", "_");
				} else if (null != fieldMap.get("name") && fieldMap.get("name").toString().length() > 0) {
					String key = fieldMap.get("name").toString();
					fieldCode += key.replaceAll("\\.", "_");
				}
			}
		}
		return fieldCode;
	}

	private String generatePageEventCode(String pElementCode, Map<String, Object> eventMap) {
		String eventCode = null;
		String type = eventMap.get("name").toString().split("=")[0];
		eventCode = pElementCode + "_" + eventMap.get("layoutCode").toString() + "_" + type;
		return eventCode;
	}

	/**
	 * 将完整的config配置信息拆分为布局信息与字段属性信息
	 * @param viewCode 所属视图或DataGrid Code
	 * @param config
	 * @param moduleCode 模块CODE
	 * @param entityCode 实体CODE
	 * @param filter TODO
	 * @return HashMap {"config":页面布局信息,"fieldConfig":字段属性信息}
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> ecSplitConfig(String viewCode, String config, String moduleCode, String entityCode, EcEnv ecEnv, boolean filter) {
		eventsList = new LinkedList<>();
		buttonsList = new LinkedList<>();
		validateList = new LinkedList<>();
		fieldsList = new LinkedList<>();

		if (null != config && config.length() > 0) {
			Map<String, Object> splitMap = new HashMap<>();
			Map<String, Object> configMap = (Map<String, Object>) SerializeUitls.deserialize(config);
			if (null != configMap && !configMap.isEmpty()) {
				if(null != configMap.get("layout")){
				Map<String, Object> layout = (Map<String, Object>) configMap.get("layout");
				if(layout != null){
				String layoutCode = (layout.get("layoutCode") == null) ? "" : layout.get("layoutCode").toString();
				if (!layout.isEmpty()) {
					LinkedList<Map<String, Object>> events = new LinkedList<>();
					Map<String, Object> pageConfig = (Map<String, Object>) layout.get("pageConfig"); // 页面信息
					List<String> filterList = Arrays.asList(FILTER_EVENTS);
					if (pageConfig != null && !pageConfig.isEmpty()) {
						for (String name : PAGE_EVENTS) {
							if(filter && filterList.contains(name)) {
								continue;
							}
							Map<String, Object> event = new HashMap<>();
							event.put("name", name);
							event.put("function", "");
							event.put("function_es5", "");
							event.put("layoutCode", layoutCode);
							event.put("code", generatePageEventCode(viewCode, event));
							event.put("moduleCode", moduleCode);
							event.put("entityCode", entityCode);
							event.put("ecEnv", ecEnv);
							if (pageConfig.containsKey(name)) {
								String function = (pageConfig.get(name) == null) ? "" : pageConfig.get(name).toString();
								String es5Name = name + "_es5";
								String function_es5 = (pageConfig.get(es5Name) == null) ? "" : pageConfig.get(es5Name).toString();
								event.put("function", function);
								event.put("function_es5", function_es5);
							}
							eventsList.add(event);
						}
					}
					List<Map<String, Object>> sections = new ArrayList<>();
					List<Map<String, Object>> tabs = (List<Map<String, Object>>) layout.get("tabs");
					if (null != tabs && !tabs.isEmpty()) {
						for (Map<String, Object> tab : tabs) {
							List<Map<String, Object>> tabSections = (List<Map<String, Object>>) tab.get("sections"); // 页签中的section
							if (tabSections != null && !tabSections.isEmpty()) {
								sections.addAll(tabSections);
							} else if(tab.get("layout") != null){
								ecSplitConfigByExtraView((List<Map<String, Object>>)tab.get("layout"),sections);
							} else if(tab.get("tabs") != null){
								ecSplitConfigByExtraView((List<Map<String, Object>>)tab.get("tabs"),sections);
							}
						}
					}
					List<Map<String, Object>> sectionSections = (List<Map<String, Object>>) layout.get("sections"); // layout下的section
					if (sectionSections != null && !sectionSections.isEmpty()) {
						sections.addAll(sectionSections);
					}
					if (sections != null && !sections.isEmpty()) {
						splitFieldConfigFromSections(viewCode, sections, events,moduleCode,entityCode);
					}
				}}
				} else if(null != configMap.get("fastQueryJson")){
					Map<String, Object> fastQueryJson = (Map<String, Object>) configMap.get("fastQueryJson");
					LinkedList<Map<String, Object>> events = new LinkedList<>();
					if (null != fastQueryJson && !fastQueryJson.isEmpty()) {
						LinkedList<Map<String, Object>> sections = new LinkedList<>();
						ArrayList<Map<String, Object>> sectionSections = (ArrayList<Map<String, Object>>) fastQueryJson.get("sections"); // layout下的section
						if (sectionSections != null && !sectionSections.isEmpty()) {
							sections.addAll(sectionSections);
						}
						if (sections != null && !sections.isEmpty()) {
							splitFieldConfigFromSectionsByExtra(viewCode, sections, events,moduleCode,entityCode);
						}
					}
				} else if(null != configMap.get("advQueryJson")){
					Map<String, Object> advQueryJson = (Map<String, Object>) configMap.get("advQueryJson");
					List<Map<String, Object>> events = new ArrayList<>();
					if (null != advQueryJson && !advQueryJson.isEmpty()) {
						List<Map<String, Object>> sections = new ArrayList<>();
						List<Map<String, Object>> sectionSections = (List<Map<String, Object>>) advQueryJson.get("sections"); // layout下的section
						if (sectionSections != null && !sectionSections.isEmpty()) {
							sections.addAll(sectionSections);
						}
						if (sections != null && !sections.isEmpty()) {
							splitFieldConfigFromSectionsByExtra(viewCode, sections, events,moduleCode,entityCode);
						}
					}
				}
			}
			return splitMap;
		}
		return null;
	}

	private boolean checkFieldFlag(String fieldName){
		if(fieldName==null || fieldName.length()==0){
			return true;
		}
		if(fieldName.equalsIgnoreCase("DELETE_STAFF") ||fieldName.equalsIgnoreCase("MODIFY_STAFF") ||fieldName.equalsIgnoreCase("MODIFY_STAFF_ID") || fieldName.equalsIgnoreCase("MODIFY_TIME") || fieldName.equalsIgnoreCase("DELETE_TIME") || fieldName.equalsIgnoreCase("DELETE_STAFF_ID") || fieldName.equalsIgnoreCase("CREATE_TIME")||fieldName.equalsIgnoreCase("CREATE_STAFF_ID")){
			return false;
		}
		return true;
	}
	public void ecSplitConfigByExtraView(List<Map<String, Object>> layouts, List<Map<String, Object>> sections) {
		for(Map<String, Object> layout : layouts){
			List<Map<String, Object>> tabSections = (List<Map<String, Object>>) layout.get("sections"); // 页签中的section
			if (tabSections != null && !tabSections.isEmpty()) {
				sections.addAll(tabSections);
			} else if(layout.get("layout") != null){
				ecSplitConfigByExtraView((List<Map<String, Object>>)layout.get("layout"),sections);
			} else if(layout.get("tabs") != null){
				ecSplitConfigByExtraView((List<Map<String, Object>>)layout.get("tabs"),sections);
			}
		}
	}
	private void splitFieldConfigFromSectionsByExtra(String pElementCode, List<Map<String, Object>> sections, List<Map<String, Object>> events, String moduleCode, String entityCode) {
		View v = null;
		FastQueryJson fastQueryJson = null;
		AdvQueryJson advQueryJson = null;
		
		if (null != sections && !sections.isEmpty()) {
			for (Map<String, Object> section : sections) {
				if (section.get("regionType") != null) {
					if(section.get("regionType").toString().equals("FASTQUERY")){
						fastQueryJson = new FastQueryJson();
						fastQueryJson.setCode(pElementCode);
						break;
					} else if(section.get("regionType").toString().equals("ADVQUERY")){
						advQueryJson = new AdvQueryJson();
						advQueryJson.setCode(pElementCode);
						break;
					}
				}
			}
		}
		if (null != sections && !sections.isEmpty()) {
			for (Map<String, Object> section : sections) {
				if (section.get("regionType") != null) {
					ArrayList<Map<String, Object>> cells = (ArrayList<Map<String, Object>>) section.get("cells");
					if (cells != null && !cells.isEmpty()) {
						if (section.get("regionType").toString().equals("FASTQUERY") || section.get("regionType").toString().equals("ADVQUERY") ) {
							for (Map<String, Object> cell : cells) {
								Map<String, Object> element = (Map<String, Object>) cell.get("element");// 获取element
								if (null != element && !element.isEmpty()) {
									TreeMap<String, Object> field = new TreeMap<>();
									String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
									String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
									field.put("cellCode", cellCode);
									field.put("regionType", regionType);
									for (Entry<String, Object> entry : element.entrySet()) {
										if ("propertyCode".equals(entry.getKey())) {
											String value = (String) entry.getValue();
											field.put("fullPropertyCode", (String) entry.getValue());
											field.put("property", null);
											if (value!=null&&value.indexOf("-") == -1) {
												if (value.indexOf("||") != -1) {
													String[] arr = value.split("\\|\\|");
													value = arr[arr.length - 1];
												}
												Property property = new Property();
												property.setCode(value);
												field.put("property", property);
											}
											
										} else if ("namekey".equals(entry.getKey())) {
											field.put("displayName", entry.getValue().toString());
										}
										if("events".equals(entry.getKey()) && (entry.getValue() != null && ((List)entry.getValue()).size() == 0)){
											continue;
										}
										
										field.put(entry.getKey(), entry.getValue());
									}
									field.put("view", v);
									if(null != fastQueryJson){
										field.put("fastQueryJson", fastQueryJson);
									} else if(null != advQueryJson){
										field.put("advQueryJson", advQueryJson);
									}
									field.put("code", generateFieldCode(pElementCode, field));
									field.put("moduleCode", moduleCode);
									field.put("entityCode", entityCode);
									EcEnv ecEnv = null;
									if (field.containsKey("ecEnv") && field.get("ecEnv") != null && !"".equals(field.get("ecEnv"))) {
										ecEnv = EcEnv.valueOf(field.get("ecEnv").toString());
									} else {
										ecEnv = PropertyHolder.getEcEnv();
									}
									ecSplitEvents((String) field.get("code"), cell, "FIELD",moduleCode,entityCode, ecEnv);
									// field.put("events", cellEvents);
									if (regionType != null && regionType.equals("EDIT")) {
										ecSplitValidate((String) field.get("code"), cell,moduleCode,entityCode);
									}
									fieldsList.addLast(field);
									cell.remove("element");// 删除布局中的字段信息
								}
							}
						}
					}
				}
			}
		}
	}
	/**
	 * 从section中拆分出fieldConfig
	 * 
	 * @param sections
	 * @param events
	 *            页面事件
	 * @return String fieldConfig
	 */
	@SuppressWarnings("unchecked")
	private void splitFieldConfigFromSections(String pElementCode, List<Map<String, Object>> sections, List<Map<String, Object>> events, String moduleCode, String entityCode) {
		DataGrid dg = null;
		View v = new View();
		v.setCode(pElementCode);
		if (null != sections && !sections.isEmpty()) {
			for (Map<String, Object> section : sections) {
				if (section.get("regionType") != null) {
					if (section.get("regionType").toString().equals("DATAGRID") && null == section.get("datagridCode")) {
						dg = new DataGrid();
						dg.setCode(pElementCode);
						v = null;
						break;
					} else if(section.get("extraView") != null){
						if(section.get("regionType").toString().equals("LISTPT") && section.get("extraView").toString().equals("true")){
							dg = new DataGrid();
							dg.setCode(pElementCode);
							v = null;
						}
					}
				}
			}
		}
		if (null != sections && !sections.isEmpty()) {
			for (Map<String, Object> section : sections) {
				if (section.get("regionType") != null) {
					List<Map<String, Object>> cells = (List<Map<String, Object>>) section.get("cells");
					if (cells != null && !cells.isEmpty()) {
						if (section.get("regionType").toString().equals("EDIT") || section.get("regionType").toString().equals("FASTQUERY")
								|| section.get("regionType").toString().equals("DIGEST") || section.get("regionType").toString().equals("ADVQUERY")) {
							for (Map<String, Object> cell : cells) {
								Map<String, Object> element = (Map<String, Object>) cell.get("element");// 获取element
								if (null != element && !element.isEmpty()) {
									TreeMap<String, Object> field = new TreeMap<>();
									String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
									String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
									field.put("cellCode", cellCode);
									field.put("regionType", regionType);
									for (Entry<String, Object> entry : element.entrySet()) {
										
										if ("propertyCode".equals(entry.getKey())) {
											String value = (String) entry.getValue();
											field.put("fullPropertyCode", (String) entry.getValue());
											field.put("property", null);
											if (value!=null&&value.indexOf("-") == -1) {
												if (value.indexOf("||") != -1) {
													String[] arr = value.split("\\|\\|");
													value = arr[arr.length - 1];
												}
												Property property = new Property();
												property.setCode(value);
												field.put("property", property);
											}
										} else if ("namekey".equals(entry.getKey())) {
											field.put("displayName", entry.getValue().toString());
										}
										//LOGGER.info("entry.getKey()++++++++++"+entry.getKey());
										field.put(entry.getKey(), entry.getValue());
									}
									field.put("view", v);
									field.put("code", generateFieldCode(pElementCode, field));
									field.put("moduleCode", moduleCode);
									field.put("entityCode", entityCode);
									EcEnv ecEnv = null;
									if (field.containsKey("ecEnv") && field.get("ecEnv") != null && !"".equals(field.get("ecEnv"))) {
										ecEnv = EcEnv.valueOf(field.get("ecEnv").toString());
									} else {
										ecEnv = PropertyHolder.getEcEnv();
									}
									ecSplitEvents((String) field.get("code"), cell, "FIELD",moduleCode,entityCode, ecEnv);
									// field.put("events", cellEvents);
									if (regionType != null && regionType.equals("EDIT")) {
										ecSplitValidate((String) field.get("code"), cell,moduleCode,entityCode);
									}
									fieldsList.addLast(field);
									cell.remove("element");// 删除布局中的字段信息
								}
							}

						} else if (section.get("regionType").toString().equals("LISTPT")
								|| section.get("regionType").toString().equals("DATAGRID")
								|| section.get("regionType").toString().equals("MNECODE")) {

							for (Map<String, Object> cell : cells) {
								TreeMap<String, Object> field = new TreeMap<>();
								if(cell != null){
								String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
								String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
								field.put("cellCode", cellCode);
								field.put("regionType", regionType);
								if (!cell.isEmpty()) {
									for (Iterator<String> it = cell.keySet().iterator(); it.hasNext();) {
										String key = it.next();
										if ("propertyCode".equals(key) && cell.get(key) != null) {
											field.put("fullPropertyCode", cell.get(key));
											field.put("property", null);
											String value = (String) cell.get(key);
											if (value.indexOf("-") == -1) {
												if (value.indexOf("||") != -1) {
													String[] arr = value.split("\\|\\|");
													value = arr[arr.length - 1];
												}
												Property property = new Property();
												property.setCode(value);
												field.put("property", property);
											}
										} else if ("namekey".equals(key)) {
											field.put("displayName", cell.get("namekey").toString());
										}
										//过滤审计字段
										if(!checkFieldFlag(key)){
											continue;
										}
										field.put(key, cell.get(key));
										if (!(key.equals("regionType") || key.equals("cellCode"))) {
											it.remove();
											cell.remove(key);
										}
									}
								}
								
								if ((section.get("regionType").toString().equals("LISTPT") && section.get("extraView") == null)
										|| section.get("regionType").toString().equals("MNECODE")) {
									field.put("view", v);
								} else if (section.get("datagridCode") == null && section.get("regionType").toString().equals("DATAGRID") 
										|| (section.get("regionType").toString().equals("LISTPT") 
												&& section.get("extraView") != null && section.get("extraView").toString().equals("true"))) {
									field.put("dataGrid", dg);
								} else if(section.get("datagridCode") != null && section.get("regionType").toString().equals("DATAGRID")){
									field.put("view", v);
								}
								field.put("code", generateFieldCode(pElementCode, field));
								EcEnv ecEnv = null;
								if (field.containsKey("ecEnv") && field.get("ecEnv") != null && !"".equals(field.get("ecEnv"))) {
									ecEnv = EcEnv.valueOf(field.get("ecEnv").toString());
								} else {
									ecEnv = PropertyHolder.getEcEnv();
								}
								if (regionType != null && regionType.equals("DATAGRID")) {
									ecSplitEvents((String) field.get("code"), field, "FIELD",moduleCode,entityCode, ecEnv);
								}
								field.put("moduleCode", moduleCode);
								field.put("entityCode", entityCode);
								
								fieldsList.addLast(field);
							}
							}
						} else if (section.get("regionType").toString().equals("BUTTON")) {

							for (Map<String, Object> cell : cells) {
								Map<String, Object> button = new HashMap<>();
								if(cell !=null){
								String cellCode = (cell.get("cellCode") == null) ? "" : cell.get("cellCode").toString();
								String regionType = (cell.get("regionType") == null) ? "" : cell.get("regionType").toString();
								button.put("regionType", regionType);
								button.put("cellCode", cellCode);
								if(v!=null) {
									button.put("view", v);
								} else {
									button.put("dataGrid", dg);
								}
								if (!cell.isEmpty()) {
									for (Iterator<String> it = cell.keySet().iterator(); it.hasNext();) {
										String key = it.next();
										Object value = cell.get(key);
										if (key.equals("id")) {
											button.put("name", value);
										} else if (key.equalsIgnoreCase("operatetype")) {
											button.put("operateType", value);
										} else if (key.equalsIgnoreCase("viewselect")) {
											button.put("viewSelect", null);
											if (value != null) {
												View viewSelect = new View();
												viewSelect.setCode((String) value);
												button.put("viewSelect", viewSelect);
											}
										} else if (key.equalsIgnoreCase("isconfirm")) {
											button.put("isConfirm", value);
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
										} else if (key.equalsIgnoreCase("ishide")) {
											button.put("isHide", value);
										} else if (key.equalsIgnoreCase("ecEnv") && value != null) {
											button.put("ecEnv", EcEnv.valueOf(value.toString()));
										} else if (key.equalsIgnoreCase("permissionCode")) {
											button.put("permissionCode", value);
										} else if (key.equalsIgnoreCase("buttonAlign")) {
											button.put("buttonAlign", value);
										} else if(key.equalsIgnoreCase("isSignatureConfig")) {
											button.put("isSignatureConfig", value);
										}else if(key.equalsIgnoreCase("releaseFelid")) {
											button.put("releaseFelid", value);
										}
										//if (!(key.equals("regionType") || key.equals("cellCode"))) {
										//	it.remove();
										//}
									}
								}
								button.put("code", generateFieldCode(pElementCode, button));
								EcEnv ecEnv = null;
								if (button.containsKey("ecEnv") && button.get("ecEnv") != null && !"".equals(button.get("ecEnv"))) {
									ecEnv = EcEnv.valueOf(button.get("ecEnv").toString());
								} else {
									ecEnv = PropertyHolder.getEcEnv();
								}
								if (regionType != null && regionType.equals("BUTTON")) {
									ecSplitEvents((String) button.get("code"), cell, "BUTTON",moduleCode,entityCode, ecEnv);
								}
								button.put("moduleCode", moduleCode);
								button.put("entityCode", entityCode);
								buttonsList.add(button);
							}
						}
					}
				}
			}
			}
		}
	}

	/**
	 * 拆分字段事件
	 * 
	 * @param cell
	 * @return List<Map> Map为单个Event属性
	 */
	private void ecSplitEvents(String pElementCode, Map<String, Object> cell, String pType, String moduleCode, String entityCode, EcEnv ecEnv) {
		if (null != cell && !cell.isEmpty()) {
			Map<String, Object> event = null;
			if (null != cell.get("funcname")) {
				String funcname = cell.get("funcname").toString();// 函数名串
				if (null != cell.get("funcbody")) {
					String funcbody = cell.get("funcbody").toString();// 函数体串
					boolean isEs5Empty = false;
					String funcbody_es5 = null;
					if (StringUtils.isEmpty((String)cell.get("funcbody_es5"))) {
						isEs5Empty = true;
					} else {
						funcbody_es5 = cell.get("funcbody_es5").toString();
					}
					String[] names = EventUtils.analysisFuncname(funcname);
					String[] bodys = EventUtils.analysisFuncbody(funcbody);
					String[] bodys_es5 = EventUtils.analysisFuncbody(funcbody_es5);
					if (names.length == bodys.length) {
						for (int i = 0; i < names.length; i++) {
							event = new HashMap<String, Object>();
							event.put("name", names[i]);
							event.put("function", bodys[i]);
							event.put("function_es5", isEs5Empty ? "" : bodys_es5[i]);
							String type = event.get("name").toString().split("=")[0];
							event.put("code", pElementCode + "_" + type);
							if ("BUTTON".equals(pType)) {
								Button button = new Button();
								button.setCode(pElementCode);
								event.put("button", button);
							} else {
								Field field = new Field();
								field.setCode(pElementCode);
								event.put("field", field);
							}
							event.put("moduleCode", moduleCode);
							event.put("entityCode", entityCode);
							event.put("ecEnv", ecEnv);
							eventsList.add(event);
						}
						cell.remove("funcname"); // 布局中除去事件
						cell.remove("funcbody");
						cell.remove("funcbody_es5");
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
					event.put("moduleCode", moduleCode);
					event.put("entityCode", entityCode);
					String type = event.get("name").toString().split("=")[0];
					event.put("code", pElementCode + "_" + type);
					event.put("ecEnv", ecEnv);

					if (!StringUtils.isEmpty(pElementCode)) {
						Field field = new Field();
						field.setCode(pElementCode);
						event.put("field", field);
					}

					eventsList.add(event);
					cell.remove("callbackname");// 布局中除去事件
					cell.remove("callbackbody");
					cell.remove("callbackbody_es5");
				}
			}
		}
	}

	/**
	 * 拆分cell中的验证信息
	 * 
	 * @param cell
	 * @return List<Map> Map为单个Validate属性
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void ecSplitValidate(String pElementCode, Map<String, Object> cell, String moduleCode, String entityCode) {
		if (null != cell && !cell.isEmpty()) {
			List<Map<String, Object>> validates = new LinkedList<Map<String, Object>>();
			if (cell.get("validate") != null && cell.get("validate") instanceof List) {
				validates = (List<Map<String, Object>>) cell.get("validate");
				cell.remove("validate");// 除去布局中的验证信息
			}
			if (validates != null && !validates.isEmpty()) {
				for (Map map : validates) {
					map.put("code", pElementCode + "_" + map.get("type"));
					Field field = new Field();
					field.setCode(pElementCode);
					map.put("field", field);

					if(map.get("param") != null) {
						map.put("params", SerializeUitls.serializeAsXml(map.get("param")));
						map.remove("param");
					}else{
						map.put("params", "");
					}
					
					if(map.get("ecEnv") != null) {
						field.setEcEnv(EcEnv.valueOf((String) map.get("ecEnv")));
					}
					map.put("moduleCode", moduleCode);
					map.put("entityCode", entityCode);
				}
				validateList.addAll(validates);
			}
		}
	}
	
	public LinkedList<Map<String, Object>> getEventsList() {
		return eventsList;
	}

	public LinkedList<Map<String, Object>> getButtonsList() {
		return buttonsList;
	}

	public LinkedList<Map<String, Object>> getValidateList() {
		return validateList;
	}

	public LinkedList<TreeMap<String, Object>> getFieldsList() {
		/*for(Map<String, Object> map : fieldsList){
			LOGGER.info("field++++++++++++++++++++"+map.keySet().toString());
		}*/
		return fieldsList;
	}

}
