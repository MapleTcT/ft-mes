/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * BAP升级专用工具类
 * 
 * @author zhuyuyin
 * @version 1.0
 */
public class XMLUpgradeUtils {
	private static final Logger logger = LoggerFactory.getLogger(XMLUpgradeUtils.class);
	private static final String BASE_MODEL_XML_PATH = "/META-INF/base/IModel.xml";
	public static final String OPERATE_TYPE_ATTRIBUTE = "operateType";
	public static final String KEY_PROPERTIES = "PROPERTIES";
	public static final String KEY_INDEXS = "INDEXS";
	public static final String KEY_ATTRIBUTES = "ATTRIBUTES";
	private static final String SQL_SEPRETOR = "@@COMMENT@@";
	private static final String[] COLUMN_INFO_FIELDS = { "TYPE_NAME", "COLUMN_SIZE", "DECIMAL_DIGITS", "NUM_PREC_RADIX" };

	/**
	 * 根据接口XML生成对应Map
	 * 
	 * @return
	 */
	public static Map<String, Object> getInterfaceMap() {
		long timestamp = System.currentTimeMillis();
		Map<String, Object> interfaceMap = new LinkedHashMap<String, Object>();
		InputStream stream = XMLUpgradeUtils.class.getResourceAsStream(BASE_MODEL_XML_PATH);
		SAXReader reader = new SAXReader();
		try {
			Document doc = reader.read(stream);
			interfaceMap = getModelAttributeMap(doc, true, null);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return interfaceMap;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getSystemCodeAttributeMap(Document doc) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Element root = doc.getRootElement();
		Iterator<Element> elements = root.elementIterator();
		while (elements.hasNext()) {
			Map<String, Object> systemEntityMap = new LinkedHashMap<String, Object>();
			Element element = elements.next();
			String code = element.attributeValue("code");
			Map<String, String> attributeMap = getAttributeMap(element);
			Iterator<Element> systemcodes = element.elementIterator("systemcode");
			Map<String, Object> systemCodeMap = new LinkedHashMap<String, Object>();
			while (systemcodes.hasNext()) {
				Element systemCodeElement = (Element) systemcodes.next();
				String systemCodeId = systemCodeElement.attributeValue("id");
				systemCodeMap.put(systemCodeId, getAttributeMap(systemCodeElement));
			}
			systemEntityMap.put("systemEntity", attributeMap);
			systemEntityMap.put("systemCode", systemCodeMap);
			map.put(code, systemEntityMap);
		}
		return map;
	}

	/**
	 * 解析XML Document 生成属性Map
	 * 
	 * @param doc
	 * @param isFull
	 *            是否全部解析为Map 包含extends的内容
	 * @param interfaceMap
	 *            接口Map
	 * @return 返回模型与字段的Map
	 *         key: 模型的唯一标示
	 *         value: Map<String,Object>
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getModelAttributeMap(Document doc, boolean isFull, Map<String, Object> fullInterfaceMap) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Element root = doc.getRootElement();
		Iterator<Element> elements = root.elementIterator();
		Map<String, String> attributeMap = null;
		Map<String, Object> propertyMap = null;
		Map<String, Object> indexMap = null;
		while (elements.hasNext()) {
			Map<String, Object> modelMap = new LinkedHashMap<String, Object>();
			attributeMap = new LinkedHashMap<String, String>();
			propertyMap = new LinkedHashMap<String, Object>();
			indexMap = new LinkedHashMap<String, Object>();
			Element element = elements.next();
			String id = element.attributeValue("id");
			attributeMap = getAttributeMap(element);
			Iterator<Element> properties = element.elementIterator("property");
			while (properties.hasNext()) {
				Element propertyElement = (Element) properties.next();
				String propertyId = propertyElement.attributeValue("id");
				propertyMap.put(propertyId, getAttributeMap(propertyElement));
			}
			Iterator<Element> indexs = element.elementIterator("index");
			while (indexs.hasNext()) {
				Element indexElement = (Element) indexs.next();
				String indexName = indexElement.attributeValue("name");
				indexMap.put(indexName, getAttributeMap(indexElement));
			}
			modelMap.put(KEY_ATTRIBUTES, attributeMap);
			modelMap.put(KEY_PROPERTIES, propertyMap);
			modelMap.put(KEY_INDEXS, indexMap);
			map.put(id, modelMap);
		}
		if (isFull) {// 全部属性，包含extends
			Map<String, Object> fullMap = new LinkedHashMap<String, Object>();
			fullMap.putAll(map);
			for (Map.Entry<String, Object> entry : fullMap.entrySet()) {
				Map<String, Object> modelMap = (Map<String, Object>) entry.getValue();
				Map<String, String> fullAttributeMap = (Map<String, String>) modelMap.get(KEY_ATTRIBUTES);
				Map<String, Object> fullPropertyMap = (Map<String, Object>) modelMap.get(KEY_PROPERTIES);
				Map<String, Object> fullIndexMap = (Map<String, Object>) modelMap.get(KEY_INDEXS);
				if (null != fullAttributeMap && !fullAttributeMap.isEmpty()) {
					if (null == fullPropertyMap) {
						fullPropertyMap = new LinkedHashMap<String, Object>();
					}
					String parentClassId = fullAttributeMap.get("extends");
					while (null != parentClassId && parentClassId.trim().length() > 0) {
						Map<String, Object> parentMap = (Map<String, Object>) map.get(parentClassId);
						boolean fromInterface = false;
						if (null == parentMap || !parentMap.isEmpty()) {// 本模型Map中没有父模型则到接口Map中找
							if (null != fullInterfaceMap && !fullInterfaceMap.isEmpty()) {
								parentMap = (Map<String, Object>) fullInterfaceMap.get(parentClassId);
								fromInterface = true;
							}
						}
						if (null == parentMap || parentMap.isEmpty()) {
							break;
						}
						Map<String, String> parentAttributeMap = (Map<String, String>) parentMap.get(KEY_ATTRIBUTES);
						Map<String, Object> parentPropertyMap = (Map<String, Object>) parentMap.get(KEY_PROPERTIES);
						Map<String, Object> parentIndexMap = (Map<String, Object>) parentMap.get(KEY_INDEXS);
						if(null != parentIndexMap && !parentIndexMap.isEmpty()){
							fullIndexMap.putAll(parentIndexMap);
						}
						if (null != parentPropertyMap && !parentPropertyMap.isEmpty()) {
							for (Map.Entry<String, Object> parentEntry : parentPropertyMap.entrySet()) {
								String propertyName = parentEntry.getKey();
								if (null == fullPropertyMap.get(propertyName)) {// 当前Map中不存在该property，直接put
									fullPropertyMap.put(propertyName, parentEntry.getValue());
								} else { // 如存在时，则解析property属性，2个property属性合并，如属性相同以本模型为主，忽略父模型的属性
									Map<String, String> fullPropertyAttributeMap = (Map<String, String>) fullPropertyMap.get(propertyName);
									Map<String, String> propertyAttributeMap = (Map<String, String>) parentEntry.getValue();
									for (Map.Entry<String, String> propertyAttributeEntry : propertyAttributeMap.entrySet()) {
										String attributeName = propertyAttributeEntry.getKey();
										if (null == fullPropertyAttributeMap.get(attributeName)) {// 原Property中不存在该属性
											fullPropertyAttributeMap.put(attributeName, propertyAttributeEntry.getValue());
										}
									}
									fullPropertyMap.put(propertyName, fullPropertyAttributeMap);
								}
							}
						}
						if (fromInterface) {// 如是从fullInterfaceMap中获取的父模型，则直接跳出循环，因为fullInterfaceMap每个已包含全部字段与属性
							break;
						}
						if (null != parentAttributeMap && !parentAttributeMap.isEmpty()) {
							parentClassId = parentAttributeMap.get("extends");
						} else {
							parentClassId = null;
						}
					}
				}
				modelMap.put(KEY_PROPERTIES, fullPropertyMap);
				modelMap.put(KEY_INDEXS, fullIndexMap);
				fullMap.put(entry.getKey(), modelMap);
			}
			map.clear();
			map.putAll(fullMap);
		}
		return map;
	}


	/**
	 * 解析Element属性，生成Map
	 * 
	 * @param element
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getAttributeMap(Element element) {
		Map<String, String> attributeMap = new LinkedHashMap<String, String>();
		Iterator<Attribute> attributes = element.attributeIterator();
		while (attributes.hasNext()) {
			Attribute attribute = (Attribute) attributes.next();
			attributeMap.put(attribute.getName(), attribute.getValue());
		}
		return attributeMap;
	}
}
