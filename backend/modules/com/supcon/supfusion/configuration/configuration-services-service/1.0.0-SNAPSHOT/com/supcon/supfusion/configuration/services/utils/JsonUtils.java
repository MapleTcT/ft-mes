/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON转换工具类.
 * 
 * @author jiawei
 * 
 */
public class JsonUtils {
	// ~ Instance fields =======================================================
	private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

	// ~ Constructor ===========================================================

	// ~ Methods ===============================================================

	public static String stringToJson(String s) {
		if (s == null) {
			return nullToJson();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if (ch >= '\u0000' && ch <= '\u001F') {
					String ss = Integer.toHexString(ch);
					sb.append("\\u");
					for (int k = 0; k < 4 - ss.length(); k++) {
						sb.append('0');
					}
					sb.append(ss.toUpperCase());
				} else {
					sb.append(ch);
				}
			}
		}
		return sb.toString();
	}

	public static String nullToJson() {
		return "";
	}

	// @SuppressWarnings("unchecked")
	// public static String objectToJson(Object obj) {
	// StringBuilder json = new StringBuilder();
	// if (obj == null) {
	// json.append("\"\"");
	// } else if (obj instanceof Number) {
	// json.append(numberToJson((Number) obj));
	// } else if (obj instanceof Boolean) {
	// json.append(booleanToJson((Boolean) obj));
	// } else if (obj instanceof String) {
	// json.append("\"").append(stringToJson(obj.toString())).append("\"");
	// } else if (obj instanceof Date) {
	// json.append("\"").append(dateToJson((Date) obj)).append("\"");
	// } else if (obj instanceof Object[]) {
	// json.append(arrayToJson((Object[]) obj));
	// } else if (obj instanceof List) {
	// json.append(listToJson((List<?>) obj));
	// } else if (obj instanceof Map) {
	// json.append(mapToJson((Map<?, ?>) obj));
	// } else if (obj instanceof Set) {
	// json.append(setToJson((Set<?>) obj));
	// } else if (obj instanceof Enum) {
	// json.append("\"").append(enumToJson((Enum) obj)).append("\"");
	// } else {
	// json.append(beanToJson(obj));
	// }
	// return json.toString();
	// }
	/**
	 * FIXME
	 */
	public static String objectToJson(Object object) {
		return objectToJson(object, null);
	}

	public static String objectToJson(Object object, String... excludes) {
		String json = null;
		// ObjectMapper mapper = new ObjectMapper();
		//
		// try {
		// json = mapper.writeValueAsString(object);
		// } catch (IOException e) {
		// throw new IllegalArgumentException(e);
		// }
		JSONSerializer serializer = new JSONSerializer();
		serializer.exclude("*.createStaff", "*.createStaffId", "*.createTime", "*.modifyStaffId", "*.deleteStaffId",
				"*.modifyStaff", "*.deleteStaff", "*.modifyTime", "*.deleteTime", "*.class", "*.company",
				"*.department", "*.manager", "*.deptManager");
		if (excludes != null && excludes.length > 0) {
			serializer.exclude(excludes);
		}

		json = serializer.serialize(object);
		return json;

	}
	
	public static String objectToJsonDeep(Object object, String ... excludes) {
		String json = null;
		JSONSerializer serializer = new JSONSerializer();
		serializer.exclude("*.createStaff", "*.createStaffId", "*.createTime", "*.modifyStaffId", "*.deleteStaffId", "*.modifyStaff",
				"*.deleteStaff", "*.modifyTime", "*.deleteTime","*.class","*.company","*.department", "*.manager", "*.deptManager");
		if(excludes != null && excludes.length > 0) {
			serializer.exclude(excludes);
		}

		json = serializer.deepSerialize(object);
		return json;

	}

	/**
	 * 将对象过滤字段后转成map，用于controller返回
	 * 
	 * @param object   待转换对象
	 * @param includes String类型，包含的字段，用逗号隔开
	 * @param excludes String类型，不包含的字段，用逗号隔开
	 * @return
	 */
	public static String objectToJson(Object object, String includes, String excludes) {
		JSONSerializer serializer = new JSONSerializer();
		if (!StringUtils.isEmpty(includes)) {
			if (includes.contains(",")) {
				String[] strs = includes.split(",");
				for (String str : strs) {
					str = str.trim();
					if (!StringUtils.isEmpty(str)) {
						serializer.include(str);
					}
				}
			} else {
				serializer.include(includes);
			}
		}
		if (!StringUtils.isEmpty(excludes)) {
			if (excludes.contains(",")) {
				String[] strs = excludes.split(",");
				for (String str : strs) {
					str = str.trim();
					if (!StringUtils.isEmpty(str)) {
						serializer.exclude(str);
					}
				}
			} else {
				serializer.exclude(excludes);
			}
		}
		return serializer.deepSerialize(object);
	}

	public static String numberToJson(Number number) {
		return number.toString();
	}

	public static String booleanToJson(Boolean bool) {
		return bool.toString();
	}

	public static String enumToJson(Enum<?> en) {
		return stringToJson(en.toString());
	}

	/**
	 * @param bean bean对象
	 * @return String
	 */
	@SuppressWarnings("unchecked")
	public static String beanToJson(Object bean) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		PropertyDescriptor[] props = null;
		try {
			Class clz = bean.getClass();
			if (bean.getClass().getName().indexOf("_$$_javassist_") < 0) {
				// 是代理 取父类
				log.debug("解析BENA－＞JSON发现一个代理");
				clz = clz.getSuperclass();
			}
			props = Introspector.getBeanInfo(clz, Object.class).getPropertyDescriptors();
		} catch (IntrospectionException e) {
		}
		if (props != null) {
			for (int i = 0; i < props.length; i++) {
				try {
					String pName = props[i].getName();
					if ("hibernateLazyInitializer".equals(pName)) {
						log.debug("发现一个hibernateLazyInitializer");
						continue;
					}
					String name = objectToJson(pName);
					Method method = props[i].getReadMethod();
					if (method == null) {
						continue;
					}
					String value = objectToJson(method.invoke(bean));
					json.append(name);
					json.append(":");
					json.append(value);
					json.append(",");
				} catch (Exception e) {
					log.error("错误出现拉\n" + e.getMessage(), e);
				}
			}
			json.setCharAt(json.length() - 1, '}');
		} else {
			json.append("}");
		}
		return json.toString();
	}

	/**
	 * @param list list对象
	 * @return String
	 */
	public static String listToJson(List<?> list) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (list != null && list.size() > 0) {
			for (Object obj : list) {
				json.append(objectToJson(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}
	
	/**
	 * @param list
	 *            list对象
	 * @return String
	 */
	public static String listToJsonDeep(List<?> list) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (list != null && list.size() > 0) {
			for (Object obj : list) {
				json.append(objectToJsonDeep(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}
	
	public static String listToJson(List<?> list, String includes, String excludes) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (list != null && list.size() > 0) {
			for (Object obj : list) {
				json.append(objectToJson(obj,includes,excludes));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}

	/**
	 * @param array 对象数组
	 * @return String
	 */
	public static String arrayToJson(Object[] array) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (array != null && array.length > 0) {
			for (Object obj : array) {
				json.append(objectToJson(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}

	/**
	 * @param map map对象
	 * @return String
	 */
	public static String mapToJson(Map<?, ?> map) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		if (map != null && map.size() > 0) {
			for (Object key : map.keySet()) {
				json.append(objectToJson(key));
				json.append(":");
				json.append(objectToJson(map.get(key)));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, '}');
		} else {
			json.append("}");
		}
		return json.toString();
	}
	
	/**
	 * @param map
	 *            map对象
	 * @return String
	 */
	public static String mapToJsonDeep(Map<?, ?> map) {
		StringBuilder json = new StringBuilder();
		json.append("{");
		if (map != null && map.size() > 0) {
			for (Object key : map.keySet()) {
				json.append(objectToJsonDeep(key));
				json.append(":");
				json.append(objectToJsonDeep(map.get(key)));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, '}');
		} else {
			json.append("}");
		}
		return json.toString();
	}

	/**
	 * @param set 集合对象
	 * @return String
	 */
	public static String setToJson(Set<?> set) {
		StringBuilder json = new StringBuilder();
		json.append("[");
		if (set != null && set.size() > 0) {
			for (Object obj : set) {
				json.append(objectToJson(obj));
				json.append(",");
			}
			json.setCharAt(json.length() - 1, ']');
		} else {
			json.append("]");
		}
		return json.toString();
	}

	public static String dateToJson(Date d) {
		return DateUtils.formatDateTime(d);
	}

	@SuppressWarnings("rawtypes")
	public static Object generateMapFromJson(String jsonStr) {
		JSONDeserializer deserializer = new JSONDeserializer();
		return deserializer.deserialize(jsonStr);
	}

	@SuppressWarnings("rawtypes")
	public static String serializeMap2Json(Map map) {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(map);
	}
}
