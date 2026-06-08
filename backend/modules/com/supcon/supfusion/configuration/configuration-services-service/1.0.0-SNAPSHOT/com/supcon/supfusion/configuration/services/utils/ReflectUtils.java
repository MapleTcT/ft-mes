/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.base.entities.SystemCode;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;

/**
 * 强力反射工具类.
 * 
 * @version 1.0
 */
public class ReflectUtils {
	// ~ Instance fields =======================================================
	private static final Logger log = LoggerFactory.getLogger(ReflectUtils.class);

	// ~ Constructor ===========================================================

	// ~ Methods ===============================================================

	/**
	 * 直接设置对象属性,public/protected/private均可,无需set方法的存在.
	 * 
	 * @param object
	 * @param fieldName
	 */
	public static void setFieldValue(final Object object, final String fieldName, final Object value) {
		Field field = getDeclaredField(object, fieldName);
		if (field == null)
			throw new IllegalArgumentException("can not find field : " + fieldName + " on object : " + object + ".");
		makeFieldAccessible(field);// 使属性可访问
		try {
			field.set(object, value);
		} catch (IllegalAccessException e) {
			log.error(e.getMessage(), e);
		}
	}


	/**
	 * 循环访问superClass直至Object,获取属性
	 * 
	 * @param object
	 * @param fieldName
	 * @return
	 */
	protected static Field getDeclaredField(final Object object, final String fieldName) {
		if (object == null)
			throw new IllegalArgumentException("object can not be null.");
		if (fieldName == null || fieldName.trim().equals(""))
			throw new IllegalArgumentException("fieldName must has value.");
		for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
			try {
				return superClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
			}
		}
		return null;
	}

	/**
	 * 获取object对象类中的所有字段（包含父类）
	 * 
	 * @param
	 * @return {@link Field }集合
	 * 
	 */
	public static List<Field> getDeepDeclaredFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		for (Class<?> superClass = clazz; superClass != Object.class; superClass = superClass.getSuperclass()) {
			fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
		}
		return fields;
	}

	/**
	 * 循环访问superClass直至Object,获取方法
	 * 
	 * @param object
	 * @param methodName
	 * @return
	 */
	protected static Method getDeclaredMethod(final Object object, final String methodName, Class clazz) {
		if (object == null)
			throw new IllegalArgumentException("object can not be null.");
		if (methodName == null || methodName.trim().equals(""))
			throw new IllegalArgumentException("fieldName must has value.");
		for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
			try {
				return superClass.getDeclaredMethod(methodName, clazz);
			} catch (SecurityException e) {
				log.warn(e.getMessage(), e);
			} catch (NoSuchMethodException e) {
			}
		}
		return null;
	}

	/**
	 * 强制field可访问.
	 * 
	 * @param field
	 */
	protected static void makeFieldAccessible(final Field field) {
		if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()))
			field.setAccessible(true);
	}


	/**
	 * 反射获取父类的泛型参数的类型.
	 * 
	 * @param clazz
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Class getSuperClassType(final Class clazz, final int index) {
		Type type = clazz.getGenericSuperclass();
		if (!(type instanceof ParameterizedType))
			return Object.class;
		Type[] params = ((ParameterizedType) type).getActualTypeArguments();
		if (index >= params.length || index < 0)
			return Object.class;
		if (!(params[index] instanceof Class))
			return Object.class;
		return (Class) params[index];
	}

	public static Map<String, Object> getDeclaredFieldValues(Object obj) throws IllegalArgumentException, IllegalAccessException {
		Map<String, Object> retMap = new HashMap<>();
		Class clazz = obj.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			if (field.isAccessible()) {
				retMap.put(field.getName(), field.get(obj));
			}
		}
		return retMap;
	}

	/**
	 * 获取对应字段的值
	 * 
	 * @param field
	 * @param object
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static Object getFieldValue(Field field, Object object) throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		return field.get(object);
	}

	/**
	 * 把对应字段设置为null
	 * 
	 * @param field
	 * @param object
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static void setFieldNVL(Field field, Object object) throws IllegalArgumentException, IllegalAccessException {
		field.setAccessible(true);
		field.set(object, null);
	}

	/**
	 * 获取对象的属性值
	 * 
	 * @param object
	 * @param properties
	 */
	public static String getPropertyValue(Object object, String properties) {
		return getPropertyValue(object, properties, Constants.SYMBOL_EMPTY);
	}

	/**
	 * 获取对象的属性值
	 * 
	 * @param object
	 * @param properties
	 * @param defaultValue
	 */
	public static String getPropertyValue(Object object, String properties, String defaultValue) {
		try {
			Object value = PropertyUtils.getProperty(object, properties);
			if (value == null)
				return defaultValue;
			String retValue = null;
			if (value instanceof Date) {
				retValue = DateUtils.formatDate((Date) value);
			} else if (value instanceof SystemCode) {
				retValue = ((SystemCode) value).getId();
			} else {
				retValue = value.toString();
			}
			return retValue;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
			return defaultValue;
		}
	}

	/**
	 * 获取对象的属性值
	 * 
	 * @param object
	 * @param properties
	 * @param
	 */
	public static Object getProperty(Object object, String properties) {
		try {
			Object value = PropertyUtils.getProperty(object, properties);
			return value;
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isExtends(Class clazz0, Class clazz1) {
		if (clazz0.getName().equals(clazz1.getName())) {
			return true;
		}
		Class subClass = clazz0.getSuperclass();
		if (null != subClass) {
			return isExtends(subClass, clazz1);
		}
		return false;
	}

	public static boolean isCollection(Class clazz) {
		if (clazz.getName().equals(Collection.class.getName())) {
			return true;
		}
		Class[] interfaces = clazz.getInterfaces();
		boolean flag = false;
		if (null != interfaces && interfaces.length > 0) {
			for (Class clazz0 : interfaces) {
				if (clazz0.getName().equals(Collection.class.getName())) {
					flag = true;
					break;
				}
			}
		}
		return flag;
	}

	/**
	 * 获取class的字段
	 *
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static Field getDeclaredField(Class<?> clazz, String fieldName) {
		Field field = null;

		for (; clazz != Object.class; clazz = clazz.getSuperclass()) {
			try {
				field = clazz.getDeclaredField(fieldName);
				return field;
			} catch (Exception e) {
				// 这里甚么都不要做！并且这里的异常必须这样写，不能抛出去。
				// 如果这里的异常打印或者往外抛，则就不会执行clazz = clazz.getSuperclass(),最后就不会进入到父类中了
			}
		}

		return null;
	}

}
