package com.supcon.supfusion.configuration.services.utils;

import flexjson.JSONDeserializer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class SerializeUitls {

	public static Map deserializeJson(String json) {
		JSONDeserializer<Map> deserializer = new JSONDeserializer<Map>();
		if (json == null || !json.startsWith("{")) {
			return Collections.EMPTY_MAP;
		}
		return deserializer.deserialize(json);
	}

	@SuppressWarnings("rawtypes")
	public static Object deserialize(String str) {
		if (null == str)
			return null;
		if (isJson(str)) {
			// json
			JSONDeserializer jd = new JSONDeserializer();
			return jd.deserialize(str);
		} else {
			// xml
			return XmlUtils.convert(str);
		}
	}

	// 提供xml操作通用类
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static String serializeAsXml(Object obj) {
		StringBuilder sb = new StringBuilder();
		String key = null;
		Object value = null;
		if (null == obj)
			return sb.toString();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<config>");
		if (obj instanceof Map) {
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) obj).entrySet()) {
				key = entry.getKey();
				value = entry.getValue();
				sb.append("<" + key.trim() + ">");
				if (obj instanceof Map || obj instanceof List) {
					sb.append(serializeAsXmlSub(value));
				} else {
					sb.append("<![CDATA[" + value + "]]>");
				}
				sb.append("</" + key.trim() + ">");
			}
		} else if (obj instanceof List) {
			sb.append("<list>");
			for (Object item : ((List) obj)) {
				sb.append("<list-item>");
				if (item instanceof Map || item instanceof List) {
					sb.append(serializeAsXmlSub(item));
				} else {
					sb.append("<![CDATA[" + value + "]]>");
				}
				sb.append("</list-item>");
			}
			sb.append("</list>");
		} else {
			sb.append("<![CDATA[" + obj + "]]>");
		}

		sb.append("</config>");
		return sb.toString().replaceAll("\"1\"", "1").replaceAll("\"undefined\"", "undefined").replaceAll("\"true\"", "true").replaceAll("\"false\"", "false");
	}

	private static String serializeAsXmlSub(Object obj) {
		StringBuilder sb = new StringBuilder();
		String key = null;
		Object value = null;
		if (null == obj)
			return sb.toString();
		if (obj instanceof Map) {
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) obj).entrySet()) {
				key = entry.getKey();
				value = entry.getValue();
				sb.append("<" + key.trim() + ">");
				if (obj instanceof Map || obj instanceof List) {
					sb.append(serializeAsXmlSub(value));
				} else {
					sb.append("<![CDATA[" + value + "]]>");
				}
				sb.append("</" + key.trim() + ">");
			}
		} else if (obj instanceof List) {
			sb.append("<list>");
			for (Object item : ((List) obj)) {
				sb.append("<list-item>");
				if (item instanceof Map || item instanceof List) {
					sb.append(serializeAsXmlSub(item));
				} else {
					sb.append("<![CDATA[" + value + "]]>");
				}
				sb.append("</list-item>");
			}
			sb.append("</list>");
		} else {
			sb.append("<![CDATA[" + obj + "]]>");
		}
		return sb.toString();
	}

	private static boolean isJson(String str) {
		return str.startsWith("{");
	}


}