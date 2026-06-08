package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.AdvQueryCondition;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.base.entities.SystemCode;
import flexjson.*;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 查询条件辅助类
 * 
 * @author 谭正阳
 * 
 */
public class ConditionUtil {

	public static final String SPLIT_DOT = ".";

	/**
	 * 根据json串生成高级查询条件
	 * 
	 * @param jsonStr
	 * @return
	 */
	public static AdvQueryCondition generateAdvQueryConditionFromJson(String jsonStr) {
		class DbColumnTypeObjectFactory implements ObjectFactory {
			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value == null) {
					return DbColumnType.TEXT;
				}
				try {
					return DbColumnType.valueOf("" + value);
				} catch (Exception e) {
					return DbColumnType.TEXT;
				}
			}
		}
		JSONDeserializer<AdvQueryCondition> deserializer = new JSONDeserializer<AdvQueryCondition>();
		deserializer.use(null, AdvQueryCondition.class).use(DbColumnType.class, new DbColumnTypeObjectFactory())
				.use("subconds.values", new AdvQueryConditionItemLocator());

		return deserializer.deserialize(jsonStr);
	}

	@SuppressWarnings("rawtypes")
	public static Object generateObjectFromJson(String jsonStr, Class clazz) {
		// 自定义boolean转换，针对boolean值两边带引号和不带引号的情况
		class BooleanObjectFactory implements ObjectFactory {
			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if (value == null) {
					return null;
				}
				try {
					return Boolean.valueOf(value.toString());
				} catch (Exception e) {
					throw new JSONException(String.format("Failed to cast string %s to boolean.", value), e);
				}
			}
		}

		// 自定义boolean转换，针对boolean值两边带引号和不带引号的情况
		class DateObjectFactory implements ObjectFactory {

			SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat fullDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				try {
					return fullDateFormatter.parse(value.toString());
				} catch (ParseException e1) {
					try {
						return simpleDateFormatter.parse(value.toString());
					} catch (ParseException e2) {
						throw new JSONException(String.format("Failed to parse %s with %s pattern.", value, simpleDateFormatter.toPattern()), e2);
					}
				}
			}
		}

		// 自定义SystemCode转换
		class SystemCodeObjectFactory implements ObjectFactory {

			@Override
			public Object instantiate(ObjectBinder context, Object value, Type targetType, Class targetClass) {
				if(value != null && value instanceof Map) {
					Map m = (Map) value;
					String id = (String) m.get("id");
					SystemCode sysCode = new SystemCode(id);
					return sysCode;
				}
				return null;
			}
		}

		JSONDeserializer deserializer = new JSONDeserializer();
		return deserializer.use("values", clazz).use(Date.class, new DateObjectFactory()).use(boolean.class, new BooleanObjectFactory())
				.use(Boolean.class, new BooleanObjectFactory()).use(SystemCode.class, new SystemCodeObjectFactory())
				.deserialize(jsonStr);
	}

	@SuppressWarnings("rawtypes")
	public static Object generateMapFromJson(String jsonStr) {
		JSONDeserializer deserializer = new JSONDeserializer();
		return deserializer.deserialize(jsonStr);
	}

	/**
	 * 将所有的自定义函数和回调函数组织起来，用于html页面的输出
	 * @param assNameMap TODO
	 * @param eventbodys
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static String[] buildEvents(View view, Map map, int flag, Map<String, String> assNameMap) {
		if(null == assNameMap){
			assNameMap = new HashMap<String, String>();
		}
		Map layout = (Map)map.get("layout");
		List tabs = (List) layout.get("tabs");
		Map<String, Map> callbackMap = new HashMap<String, Map>();// 源模型属性和存放回调函数的map
		Map<String, String> callbackfuncMap = new HashMap<String, String>();// 存放回调函数体，以及回调函数要回填的参数。
		StringBuilder eventsb = new StringBuilder();
		if (null != tabs && !tabs.isEmpty()) {
			Map tab, section, formElement, elementProperty;
			List sections, formElements;
			String eventbody, callbackbody, sourcepropertyname, propertyName,topAssName;
			for (int i = 0; i < tabs.size(); i++) {
				tab = (Map) tabs.get(i);
				sections = (List) tab.get("sections");
				if (null != sections && !sections.isEmpty()) {
					for (int k = 0; k < sections.size(); k++) {
						section = (Map) sections.get(k);
						if (null != section && !section.isEmpty()) {
							formElements = (List) section.get("cells");
							if (null != formElements && !formElements.isEmpty()) {
								for (int j = 0; j < formElements.size(); j++) {
									formElement = (Map) formElements.get(j);
									if (null != formElement && !formElement.isEmpty()) {
										// 连接所有的自定义事件
										eventbody = (String) formElement.get("funcbody");
										if (null != eventbody && !eventbody.isEmpty()) {
											eventsb.append("@@@@").append(eventbody);
										}
										elementProperty = (Map) formElement.get("element");
										if (null != elementProperty && !elementProperty.isEmpty()) {
											// 组织默认回调函数及其内容，用于生成回调函数，Start
											// 组成的形式是callbackMap{主模型的属性名称=callbackfuncMap}，
											// callbackfuncMap{主模型的属性名称+“/”=callbackbody,主模型的属性名称+“funcparam”=关联属性的字段串用|连接}
											callbackbody = (String) formElement.get("callbackbody");
											propertyName = (String) elementProperty.get("name");
											sourcepropertyname = (String) formElement.get("sourcepropertyname");
											topAssName = (String) formElement.get("topAssName");
											if (null != sourcepropertyname) {
												Map<String, String> callbackfuncTemp = new HashMap<String, String>();
												if (null != callbackMap.get(sourcepropertyname.split("\\-")[0])) {
													propertyName = callbackfuncMap
															.get(sourcepropertyname.split("\\-")[0] + "funcparam") + "|" + propertyName;
												}
												if (null != callbackbody && !callbackbody.isEmpty()) {
													callbackfuncMap.put(sourcepropertyname.split("\\-")[0] + "funcbody", callbackbody);
												}
												callbackfuncMap.put(sourcepropertyname.split("\\-")[0] + "funcparam", propertyName);
												callbackfuncTemp.put(sourcepropertyname.split("\\-")[0] + "funcparam",
														callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcparam"));
												callbackfuncTemp.put(sourcepropertyname.split("\\-")[0] + "funcbody",
														callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcbody"));
												callbackMap.put(sourcepropertyname.split("\\-")[0], callbackfuncTemp);
												if(null != topAssName){
													assNameMap.put(sourcepropertyname.split("\\-")[0], topAssName);
												}
											}
											// 组织默认回调函数及其内容，用于生成回调函数，END
											// 有效性验证函数组织
											if (!elementProperty.get("showType").equals("LABEL")
													&& !elementProperty.get("showType").equals("DATAGRID") && flag == 0) {
												// validfunction += validDate(elementProperty);
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
		// validfunction += "@@@@return true;@@@@}";
		String[] callbackFuncFinal = buildcallback(callbackMap, "MAIN");// 返回组织好的回调函数和需要回填的注释字段
		eventsb.append(callbackFuncFinal[0].toString());// 将自定义函数和回调函数组织起来
		// eventsb.append(validfunction);
		String eventbodys = eventsb.toString();
		eventbodys = eventbodys.replaceAll("@@double_quote@@", "##double_quote##").replaceAll("@@single_quote@@", "##single_quote##")
				.replace("@@@@", "\r\n").replaceAll("##double_quote##", "@@double_quote@@")
				.replaceAll("##single_quote##", "@@single_quote@@").replace("<br/>", "\r\n");
		return new String[] { eventbodys, callbackFuncFinal[1].toString() };
	}

	@SuppressWarnings("rawtypes")
	public static String[] buildExtraViewEvents(View view, Map map, int flag, Map<String, String> assNameMap) {
		if(null == assNameMap){
			assNameMap = new HashMap<String, String>();
		}
		Map layout = (Map)map.get("layout");
		List tabs = (List) layout.get("tabs");
		Map<String, Map> callbackMap = new HashMap<String, Map>();// 源模型属性和存放回调函数的map
		Map<String, String> callbackfuncMap = new HashMap<String, String>();// 存放回调函数体，以及回调函数要回填的参数。
		List sections = new ArrayList();
		StringBuilder eventsb = new StringBuilder();
		if (null != tabs && !tabs.isEmpty()) {
			Map tab, section, formElement, elementProperty, layoutProperties;
			List formElements;
			String eventbody, callbackbody, sourcepropertyname, propertyName,topAssName;
			for (int i = 0; i < tabs.size(); i++) {
				tab = (Map) tabs.get(i);
				layoutProperties = (Map) tab.get("layoutProperties");
				if(null != layoutProperties && !layoutProperties.isEmpty()){
					String layoutMethod = (String) layoutProperties.get("layoutmethod");
					if(null != layoutMethod && !"".equals(layoutMethod)){
						buildExtraViewEventsTranfer(tab, sections, layoutMethod);
					}
				}
			}
			if (null != sections && !sections.isEmpty()) {
				for (int k = 0; k < sections.size(); k++) {
					section = (Map) sections.get(k);
					if (null != section && !section.isEmpty()) {
						formElements = (List) section.get("cells");
						if (null != formElements && !formElements.isEmpty()) {
							for (int j = 0; j < formElements.size(); j++) {
								formElement = (Map) formElements.get(j);
								if (null != formElement && !formElement.isEmpty()) {
									// 连接所有的自定义事件
									eventbody = (String) formElement.get("funcbody");
									if (null != eventbody && !eventbody.isEmpty()) {
										eventsb.append("@@@@").append(eventbody);
									}
									elementProperty = (Map) formElement.get("element");
									if (null != elementProperty && !elementProperty.isEmpty()) {
										// 组织默认回调函数及其内容，用于生成回调函数，Start
										// 组成的形式是callbackMap{主模型的属性名称=callbackfuncMap}，
										// callbackfuncMap{主模型的属性名称+“/”=callbackbody,主模型的属性名称+“funcparam”=关联属性的字段串用|连接}
										callbackbody = (String) formElement.get("callbackbody");
										propertyName = (String) elementProperty.get("name");
										sourcepropertyname = (String) formElement.get("sourcepropertyname");
										topAssName = (String) formElement.get("topAssName");
										if (null != sourcepropertyname) {
											Map<String, String> callbackfuncTemp = new HashMap<String, String>();
											if (null != callbackMap.get(sourcepropertyname.split("\\-")[0])) {
												propertyName = callbackfuncMap
														.get(sourcepropertyname.split("\\-")[0] + "funcparam") + "|" + propertyName;
											}
											if (null != callbackbody && !callbackbody.isEmpty()) {
												callbackfuncMap.put(sourcepropertyname.split("\\-")[0] + "funcbody", callbackbody);
											}
											callbackfuncMap.put(sourcepropertyname.split("\\-")[0] + "funcparam", propertyName);
											callbackfuncTemp.put(sourcepropertyname.split("\\-")[0] + "funcparam",
													callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcparam"));
											callbackfuncTemp.put(sourcepropertyname.split("\\-")[0] + "funcbody",
													callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcbody"));
											callbackMap.put(sourcepropertyname.split("\\-")[0], callbackfuncTemp);
											if(null != topAssName){
												assNameMap.put(sourcepropertyname.split("\\-")[0], topAssName);
											}
										}
										// 组织默认回调函数及其内容，用于生成回调函数，END
										// 有效性验证函数组织
										if (!elementProperty.get("showType").equals("LABEL")
												&& !elementProperty.get("showType").equals("DATAGRID") && flag == 0) {
											// validfunction += validDate(elementProperty);
										}
									}
								}
							}
						}
					
					}
				}
			}
		}
		// validfunction += "@@@@return true;@@@@}";
		String[] callbackFuncFinal = buildcallback(callbackMap, "MAIN");// 返回组织好的回调函数和需要回填的注释字段
		eventsb.append(callbackFuncFinal[0].toString());// 将自定义函数和回调函数组织起来
		// eventsb.append(validfunction);
		String eventbodys = eventsb.toString();
		eventbodys = eventbodys.replaceAll("@@double_quote@@", "##double_quote##").replaceAll("@@single_quote@@", "##single_quote##")
				.replace("@@@@", "\r\n").replaceAll("##double_quote##", "@@double_quote@@")
				.replaceAll("##single_quote##", "@@single_quote@@").replace("<br/>", "\r\n");
		return new String[] { eventbodys, callbackFuncFinal[1].toString() };
	}
	
	public static void buildExtraViewEventsTranfer(Map map, List sections, String layoutMethod) {
		List layouts;
		Map layoutProperties,layout;
		if ("row".equals(layoutMethod) || "column".equals(layoutMethod)) {
			layouts = (List) map.get("layout");
			for (int i = 0; i < layouts.size(); i++) {
				layout = (Map) layouts.get(i);
				layoutProperties = (Map) layout.get("layoutProperties");
				if(null != layoutProperties && !layoutProperties.isEmpty()){
					String innerlayoutMethod = (String) layoutProperties.get("layoutmethod");
					if(null != innerlayoutMethod && !"".equals(innerlayoutMethod)){
						buildExtraViewEventsTranfer(layout, sections, innerlayoutMethod);
					}
				}
			}
		} else if ("tab".equals(layoutMethod)) {
			layouts = (List) map.get("tabs");
			for (int i = 0; i < layouts.size(); i++) {
				layout = (Map) layouts.get(i);
				layoutProperties = (Map) layout.get("layoutProperties");
				if(null != layoutProperties && !layoutProperties.isEmpty()){
					String innerlayoutMethod = (String) layoutProperties.get("layoutmethod");
					if(null != innerlayoutMethod && !"".equals(innerlayoutMethod)){
						buildExtraViewEventsTranfer(layout, sections, innerlayoutMethod);
					}
				}
			}
		} else if ("container".equals(layoutMethod)) {
			layoutProperties = (Map) map.get("layoutProperties");
			if(null != layoutProperties && !layoutProperties.isEmpty()){
				String layoutContent = (String) layoutProperties.get("layoutContent");
				if(null != layoutContent && "section".equals(layoutContent)){
					if(null != map.get("sections")){
						sections.addAll((List)map.get("sections"));
					}
				}
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String[] buildFastQueryEvent(Map fastMap) {
		Map<String, Map> callbackMap = new HashMap<String, Map>();// 源模型属性和存放回调函数的map
		Map<String, String> callbackfuncMap = new HashMap<String, String>();// 存放回调函数体，以及回调函数要回填的参数。
		StringBuilder eventsb = new StringBuilder();
		Map section, formElement, elementProperty;
		List sections, formElements;
		String eventbody, callbackbody, sourcepropertyname, propertyName;
		
		Map layout = (Map)fastMap.get("layout");
		if(layout!=null && !layout.isEmpty()){
			sections = (List<Map>)layout.get("sections");
			if(sections!=null && !sections.isEmpty()){
				for (int k = 0; k < sections.size(); k++) {
					section = (Map) sections.get(k);
					if (null != section && !section.isEmpty() && section.get("regionType") != null
							&& section.get("regionType").toString().equals("FASTQUERY")) {
						formElements = (List) section.get("cells");
						if (null != formElements && !formElements.isEmpty()) {
							for (int j = 0; j < formElements.size(); j++) {
								formElement = (Map) formElements.get(j);
								if (null != formElement && !formElement.isEmpty()) {
									// 连接所有的自定义事件
									eventbody = (String) formElement.get("funcbody");
									if (null != eventbody && !eventbody.isEmpty()) {
										eventsb.append("@@@@").append(eventbody);
									}
									elementProperty = (Map) formElement.get("element");
									if (null != elementProperty && !elementProperty.isEmpty()) {
										// 组织默认回调函数及其内容，用于生成回调函数，Start
										// 组成的形式是callbackMap{主模型的属性名称=callbackfuncMap}，
										// callbackfuncMap{主模型的属性名称+“/”=callbackbody,主模型的属性名称+“funcparam”=关联属性的字段串用|连接}
										callbackbody = (String) formElement.get("callbackbody");
										propertyName = (String) elementProperty.get("name");
										sourcepropertyname = (String) formElement.get("sourcepropertyname");
										if (null != sourcepropertyname) {
											Map<String, String> callbackfuncTemp = new HashMap<String, String>();
											if (null != callbackMap.get(sourcepropertyname.split("\\-")[0])) {
												propertyName = callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcparam") + "|"
														+ propertyName;
											}
											if (null != callbackbody && !callbackbody.isEmpty()) {
												callbackfuncMap.put(sourcepropertyname.split("\\-")[0] + "funcbody", callbackbody);
											}
											callbackfuncMap.put(sourcepropertyname.split("\\-")[0] + "funcparam", propertyName);
											callbackfuncTemp.put(sourcepropertyname.split("\\-")[0] + "funcparam",
													callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcparam"));
											callbackfuncTemp.put(sourcepropertyname.split("\\-")[0] + "funcbody",
													callbackfuncMap.get(sourcepropertyname.split("\\-")[0] + "funcbody"));
											callbackMap.put(sourcepropertyname.split("\\-")[0], callbackfuncTemp);
										}
										// 组织默认回调函数及其内容，用于生成回调函数，END

									}
								}
							}
						}
					
					}
				}
			
			}
		}
			
		String[] callbackFuncFinal = ConditionUtil.buildcallback(callbackMap, "MAIN");// 返回组织好的回调函数和需要回填的注释字段
		eventsb.append(callbackFuncFinal[0].toString());// 将自定义函数和回调函数组织起来
		String eventbodys = eventsb.toString();
		eventbodys = eventbodys.replace("@@@@", "\r\n").replace("<br/>", "\r\n");
		return new String[] { eventbodys, callbackFuncFinal[1].toString() };
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String buildExtraFastQueryEvent(Map fastMap) {
		StringBuilder eventsb = new StringBuilder();
		Map section, formElement;
		List sections, formElements;
		String eventbody;
		
		Map layout = (Map)fastMap.get("fastQueryJson");
		if(layout!=null && !layout.isEmpty()){
			sections = (List<Map>)layout.get("sections");
			if(sections!=null && !sections.isEmpty()){
				for (int k = 0; k < sections.size(); k++) {
					section = (Map) sections.get(k);
					if (null != section && !section.isEmpty() && section.get("regionType") != null
							&& section.get("regionType").toString().equals("FASTQUERY")) {
						formElements = (List) section.get("cells");
						if (null != formElements && !formElements.isEmpty()) {
							for (int j = 0; j < formElements.size(); j++) {
								formElement = (Map) formElements.get(j);
								if (null != formElement && !formElement.isEmpty()) {
									// 连接所有的自定义事件
									eventbody = (String) formElement.get("funcbody");
									if (null != eventbody && !eventbody.isEmpty()) {
										eventsb.append("@@@@").append(eventbody);
									}
								}
							}
						}
					}
				}
			
			}
		}
			
		String eventbodys = eventsb.toString();
		eventbodys = eventbodys.replace("@@@@", "\r\n").replace("<br/>", "\r\n");
		return eventbodys;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String buildExtraAdvQueryEvent(Map fastMap) {
		StringBuilder eventsb = new StringBuilder();
		Map section, formElement;
		List sections, formElements;
		String eventbody;
		
		Map layout = (Map)fastMap.get("advQueryJson");
		if(layout!=null && !layout.isEmpty()){
			sections = (List<Map>)layout.get("sections");
			if(sections!=null && !sections.isEmpty()){
				for (int k = 0; k < sections.size(); k++) {
					section = (Map) sections.get(k);
					if (null != section && !section.isEmpty() && section.get("regionType") != null
							&& section.get("regionType").toString().equals("ADVQUERY")) {
						formElements = (List) section.get("cells");
						if (null != formElements && !formElements.isEmpty()) {
							for (int j = 0; j < formElements.size(); j++) {
								formElement = (Map) formElements.get(j);
								if (null != formElement && !formElement.isEmpty()) {
									// 连接所有的自定义事件
									eventbody = (String) formElement.get("funcbody");
									if (null != eventbody && !eventbody.isEmpty()) {
										eventsb.append("@@@@").append(eventbody);
									}
								}
							}
						}
					}
				}
			
			}
		}
			
		String eventbodys = eventsb.toString();
		eventbodys = eventbodys.replace("@@@@", "\r\n").replace("<br/>", "\r\n");
		return eventbodys;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String buildDataGridEvent(Map datagridMap) {
		Map<String, Map> callbackMap = new HashMap<String, Map>();// 源模型属性和存放回调函数的map
		StringBuilder eventsb = new StringBuilder();
		String eventbody, callbackbody;
		Map layout = (Map)datagridMap.get("layout");
		if(null != layout && !layout.isEmpty()){
			List<Map> sections = (List<Map>) layout.get("sections");
			if(null != sections && !sections.isEmpty()){
				for(Map section : sections){
					if(section.get("regionType")!=null && section.get("regionType").toString().equals("DATAGRID")){
						List<Map> cells = (List<Map>) section.get("cells");
						if(null != cells && !cells.isEmpty()){
							for(Map col : cells){
								if (null != col && !col.isEmpty()) {
									// 连接所有的自定义事件
									eventbody = (String) col.get("funcbody");
									callbackbody = (String) col.get("callbackbody");
									if (null != eventbody && !eventbody.isEmpty()) {
										eventsb.append("@@@@").append(eventbody);
									}
									if (null != callbackbody && !callbackbody.isEmpty()) {
										eventsb.append("@@@@").append(callbackbody);
									}
								}
							}
						}
						break;
					}
				}
				String[] callbackFuncFinal = ConditionUtil.buildcallback(callbackMap, "MAIN");// 返回组织好的回调函数和需要回填的注释字段
				eventsb.append(callbackFuncFinal[0].toString());// 将自定义函数和回调函数组织起来
				String eventbodys = eventsb.toString();
				eventbodys = eventbodys.replace("@@@@", "\r\n").replace("<br/>", "\r\n");
				return eventbodys;
			}
		}
		
		return "";
	}

	/**
	 * 自动生成回调函数
	 * 
	 * @param tagProperty
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String[] buildcallback(Map<String, Map> callbackMap, String flag) {
		// StringBuilder callbacksql = new StringBuilder();// 组织回调函数的sql
		Map<String, String> callbackfunc = new HashMap<String, String>();
		String sourcename, funcbody, funcparam, callbackstr = "@@@@", sourcenamestr = "";
		// 分离回调函数的map，根据回调函数的函数和相关需要回调的参数生成最终的回调函数函数
		for (Map.Entry<String, Map> entry : callbackMap.entrySet()) {
			// 先清空，主实体（包括其下级属性）中一个属性只能有一个回调函数
			sourcename = entry.getKey().toString();
			sourcenamestr += "|" + entry.getKey().toString();
			callbackfunc = entry.getValue();
			funcbody = callbackfunc.get(sourcename + "funcbody");
			funcparam = callbackfunc.get(sourcename + "funcparam");
			if (null != funcbody && null != funcparam) {

				funcbody = funcbody.substring(0, funcbody.length() - 1);// 去掉最后一个大括号，准备将参数组织进去。
				callbackstr += funcbody + "@@@@}@@@@";

			}
		}
		if (null != sourcenamestr && !"".equals(sourcenamestr.trim())) {
			sourcenamestr = sourcenamestr.toString().substring(1);
		}
		return new String[] { callbackstr.toString(), sourcenamestr };

	}

	public static Map<String, String> resolve(HttpServletRequest req) {
		Map<String, String> model = new HashMap<String, String>();
		// String key;
		String[] values;

		for (Object key : req.getParameterMap().keySet()) {
			values = req.getParameterValues((String) key);
			if (null != values) {
				int i = 0;
				for (i = 0; i < values.length; i++) {
					if (null != values[i] && !"".equals(values[i].trim())) {
						model.put((String) key, values[i]);
						break;
					}
				}
			}
		}

		return model;
	}

	/**
	 * 对源属性的tagPropertyMap进行排序，保证生成的SQL按照层次顺序排列
	 * 
	 * @param tagProperty
	 * @return
	 */
	public static Map<String, String> orderAssProperty(Map<String, String> tagProperty) {
		Map<String, String> tempOrder = new TreeMap<String, String>(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int l1 = o1.split("\\.").length;
				int l2 = o2.split("\\.").length;
				int result = l1 - l2;
				if (0 == result)
					return o1.compareTo(o2);
				return result;
			}
		});
		// 关联关系的map转化成string形式，因为源属性的key记录成从主实体到本身源实体的路径形式。如：A-B-C=X。这里需要进行分割成A=B，B=C，C=X的形式，再放入map中排序
		String assPropertyStr;
		String PropertyArr[], PropertyLayRec[];
		assPropertyStr = "";
		// 将整个Map组成一个字符串进行分给后再放入到treemap中进行排序
		for (Map.Entry<String, String> entry : tagProperty.entrySet()) {
			PropertyLayRec = entry.getKey().toString().split("\\-");
			String tempKey = "";// 转换上述形式
			for (int i = 0; i < PropertyLayRec.length; i++) {
				if (i > 0) {
					tempKey += "," + PropertyLayRec[i - 1] + "=" + PropertyLayRec[i];
				}
				if (i == PropertyLayRec.length - 1) {
					tempKey += "," + PropertyLayRec[i];
				}
			}
			assPropertyStr += tempKey + "=" + entry.getValue().toString();
		}
		assPropertyStr = assPropertyStr.substring(1);
		PropertyArr = assPropertyStr.split("\\,");
		// 将map串放入到有序的map中
		for (int i = 0; i < PropertyArr.length; i++) {
			String[] tempEntity = PropertyArr[i].split("\\=");
			if (null != tempOrder.get(tempEntity[0])) {
				tempOrder.put(tempEntity[0], tempOrder.get(tempEntity[0]) + "|" + tempEntity[1]);
			} else {
				tempOrder.put(tempEntity[0], tempEntity[1]);
			}
		}
		return tempOrder;
	}

	/**
	 * 将DataGrid中获取的数据转化为List<Map>形式返回
	 * 
	 * @param parameters
	 * @return
	 */

	public static Object[] executeDataGridParameters(Map<String, String> parameters) {
		Assert.notNull(parameters);
		Map<String, Map<Integer, Map<String, Object[]>>> records = new HashMap<String, Map<Integer, Map<String, Object[]>>>();
		Map<String, String> deleteRecords = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			String key = entry.getKey();
			String[] keys = key.split("___");
			String value = entry.getValue();
			Integer index = Integer.parseInt(keys[1]);
			if (keys[2].toString().equals("delRowIDS")) {
				deleteRecords.put(keys[0].toString(), value);
				continue;
			}
			Map<Integer, Map<String, Object[]>> recordMap = records.get(keys[0]);
			if (recordMap == null) {
				recordMap = new HashMap<Integer, Map<String, Object[]>>();
				Map<String, Object[]> m = new HashMap<String, Object[]>();
				m.put(keys[2].replace("__", "."), new Object[] { value, null });
				recordMap.put(index, m);
				records.put(keys[0], recordMap);
			} else {
				Map<String, Object[]> m = recordMap.get(index);
				if (m == null) {
					m = new HashMap<String, Object[]>();
					m.put(keys[2].replace("__", "."), new Object[] { value, null });
					recordMap.put(index, m);
				} else {
					m.put(keys[2].replace("__", "."), new Object[] { value, null });
				}
			}
		}

		return new Object[] { records, deleteRecords };
	}

	/**
	 * 组织DataGrid中的各种事件
	 * 
	 * @param deseAll
	 *            view
	 * @return
	 */

	@SuppressWarnings("rawtypes")
	public static String buildDTEvent(View view, Map deseAll) {
		List columns = (List) deseAll.get("columns");
		Map<String, Map> callbackMap = new HashMap<String, Map>();// 源模型属性和存放回调函数的map
		Map<String, String> callbackfuncMap = new HashMap<String, String>();// 存放回调函数体，以及回调函数要回填的参数。
		String eventbody, callbackbody, sourcepropertyname, propertyName;
		StringBuilder eventsb = new StringBuilder();
		Map column;
		for (int i = 0; i < columns.size(); i++) {
			column = (Map) columns.get(i);
			if (null != column && !column.isEmpty()) {
				eventbody = (String) column.get("funcbody");
				if (null != eventbody && !eventbody.isEmpty()) {
					eventsb.append("@@@@").append(eventbody);
				}
				callbackbody = (String) column.get("callbackbody");
				propertyName = (String) column.get("key");
				sourcepropertyname = (String) column.get("sourcepropertyname");
				if (null != sourcepropertyname) {
					Map<String, String> callbackfuncTemp = new HashMap<String, String>();
					if (null != callbackMap.get(sourcepropertyname)) {
						propertyName = callbackfuncMap.get(sourcepropertyname + "funcparam") + "|" + propertyName;
					}
					if (null != callbackbody && !callbackbody.isEmpty()) {
						callbackfuncMap.put(sourcepropertyname + "funcbody", callbackbody);
					}
					callbackfuncMap.put(sourcepropertyname + "funcparam", propertyName);
					callbackfuncTemp.put(sourcepropertyname + "funcparam", callbackfuncMap.get(sourcepropertyname + "funcparam"));
					callbackfuncTemp.put(sourcepropertyname + "funcbody", callbackfuncMap.get(sourcepropertyname + "funcbody"));
					callbackMap.put(sourcepropertyname, callbackfuncTemp);
				}
			}
		}
		String[] callbackFuncFinal = buildcallback(callbackMap, "DT");// 返回组织好的回调函数和需要回填的注释字段
		eventsb.append(callbackFuncFinal[0]);
		return eventsb.toString();
	}

	@SuppressWarnings("rawtypes")
	public static Map deserializeJson(String json) {
		JSONDeserializer<Map> deserializer = new JSONDeserializer<Map>();
		if (json == null || !json.startsWith("{")) {
			return Collections.EMPTY_MAP;
		}
		return deserializer.deserialize(json);
	}

	public static String serialize(Object obj) {
		return serialize(obj, null, null);
	}

	public static String serialize(Object obj, String includes, String excludes) {
		JSONSerializer serializer = new JSONSerializer();
		if (obj == null) {
			return "";
		}
		StringTokenizer tokenizer = null;
		if (includes != null) {
			tokenizer = new StringTokenizer(includes, ",");
			while (tokenizer.hasMoreTokens()) {
				String item = tokenizer.nextToken();
				if (item != null && item.length() > 0) {
					serializer.include(item);
				}
			}
		}
		if (excludes != null) {
			tokenizer = new StringTokenizer(excludes, ",");
			while (tokenizer.hasMoreTokens()) {
				String item = tokenizer.nextToken();
				if (item != null && item.length() > 0) {
					serializer.exclude(item);
				}
			}
		}
		return serializer.deepSerialize(obj);
	}

}
