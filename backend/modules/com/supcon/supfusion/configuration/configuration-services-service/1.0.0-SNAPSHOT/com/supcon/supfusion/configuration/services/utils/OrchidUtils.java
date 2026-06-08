/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.annotation.BAPIsMainDisplay;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class OrchidUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(OrchidUtils.class);

	/**
	 * Encodes the powercode using {@link BAPUrlBase64#encode(byte[])} with the encoding
	 * specified in the configuration.
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] encode(byte[] input) {
		return BAPUrlBase64.encode(input);
	}


	public static String getMainDisplayValue(Object entity) {
		Class<?> clazz = Hibernate.getClass(entity);
		try {
			String getName = "getId";
			for (Field f : clazz.getDeclaredFields()) {
				if (f.isAnnotationPresent(BAPIsMainDisplay.class)) { // 被标注为是否是主显示字段
					String fieldName = f.getName();
					getName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
					break;
				}
			}
			Method getMethod = clazz.getMethod(getName);
			String value = getMethod.invoke(entity).toString();
			return value;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return "";
	}
	
}
