package com.supcon.supfusion.configuration.services.service;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface ReflectService {

	Method getMethod(Class clazz, String methodName, Class<?> type1, Class<?> type2) throws NoSuchMethodException, SecurityException;

	Field getField(Class clazz, String fieldName) throws NoSuchFieldException, SecurityException;

	Field getDeepField(Class clazz, String fieldName);

	Object getFieldValue(Object object, String fieldName) throws Exception;


	Object getStaticFieldValue(Class clazz, String fieldName) throws Exception;
}
