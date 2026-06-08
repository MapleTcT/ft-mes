package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.utils.ReflectUtils;
import com.supcon.supfusion.configuration.services.service.ReflectService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/27
 */
@Slf4j
@ServiceApiService
@Transactional
public class ReflectServiceImpl implements ReflectService {

    @Override
    public Method getMethod(Class clazz, String methodName, Class<?> type1, Class<?> type2) throws NoSuchMethodException, SecurityException {
        Method method = null;
        try {
            if (type1 == null) {
                method = clazz.getDeclaredMethod(methodName);
            } else {
                method = clazz.getDeclaredMethod(methodName, type1);
            }
        } catch (NoSuchMethodException e) {
            if (type2 == null) {
                method = clazz.getMethod(methodName);
            } else {
                method = clazz.getMethod(methodName, type2);
            }
        }
        return method;
    }

    @Override
    public Field getField(Class clazz, String fieldName) throws NoSuchFieldException, SecurityException {
        Field f = null;
        try {
            f = clazz.getDeclaredField(fieldName);
        } catch (Exception e) {
            log.warn(e.getMessage(),e);
        }
        return f;
    }

    @Override
    public Field getDeepField(Class clazz, String fieldName) {
        Field f = null;
        try {
            f = ReflectUtils.getDeclaredField(clazz, fieldName);
        } catch (Exception e) {
            log.warn(e.getMessage(),e);
        }
        return f;
    }

    @Override
    public Object getFieldValue(Object object, String fieldName) throws Exception {
        Field field = getDeepField(object.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        return ReflectUtils.getFieldValue(field, object);
    }

    @Override
    public Object getStaticFieldValue(Class clazz, String fieldName) throws Exception {
        java.lang.reflect.Field field = clazz.getField(fieldName);
        if (null == field) {
            return null;
        }
        if (Modifier.isStatic(field.getModifiers())) {
            return field.get(null);
        }
        return null;
    }
}
