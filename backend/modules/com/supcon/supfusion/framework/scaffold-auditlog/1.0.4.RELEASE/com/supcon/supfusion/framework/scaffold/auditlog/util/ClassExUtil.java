package com.supcon.supfusion.framework.scaffold.auditlog.util;

import com.supcon.supfusion.framework.scaffold.auditlog.annotation.AuditJoinPK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 类操作工具
 *
 * @author caokele
 */
public class ClassExUtil extends ObjectUtils {
    private static final Logger logger = LoggerFactory.getLogger(ClassExUtil.class);

    /**
     * 名为id的字段
     */
    public static final String FIELD_ID = "id";
    /**
     * 名为code的字段
     */
    public static final String FIELD_CODE = "code";
    /**
     * 名为name的字段
     */
    public static final String FIELD_NAME = "name";

    /**
     * 文件路径字段
     */
    public static final String FIELD_FILE_PATH = "filePath";

    /**
     * 将模型对象转换为map
     * 会忽略空值字段、静态字段、集合字段、数组字段
     * 如果是实体对象字段，会获取实体对象的主键
     */
    public static Map<String, Object> modelToMap(@NonNull Object model) throws IllegalAccessException {
        Class<?> modelClazz = model.getClass();
        // 模型所有字段
        List<Field> allFields = new LinkedList<>();
        Map<String, Object> modelMap = new HashMap<>(allFields.size());
        //向上循环 遍历父类，获取所有字段
        for (; modelClazz != null && modelClazz != Object.class; modelClazz = modelClazz.getSuperclass()) {
            Field[] fields = modelClazz.getDeclaredFields();
            if (fields.length > 0) {
                allFields.addAll(Arrays.asList(fields));
            }
        }
        for (Field field : allFields) {
            field.setAccessible(true);
            // 字段名称
            String fieldName = field.getName();
            // 字段类型
            Class<?> fieldClazz = field.getType();
            // 字段值
            Object value = field.get(model);
            // 是否是需要忽略的字段
            if (isIgnoreFiled(field, value)) {
                continue;
            }
            // 如果是基本类、包装类或字符串
            if (ClassUtils.isPrimitiveOrWrapper(fieldClazz) || String.class == fieldClazz || ClassUtils.isPrimitiveArray(fieldClazz)) {
                modelMap.put(fieldName, value);
                continue;
            }
            // 如果是Date，存储时间戳
            if (Date.class == fieldClazz) {
                Date date = (Date) value;
                modelMap.put(fieldName, date.getTime());
                continue;
            }
            // 如果是BigDecimal或枚举类型
            if (BigDecimal.class == fieldClazz || fieldClazz.isEnum()) {
                modelMap.put(fieldName, value.toString());
                continue;
            }
            // 如果是对象，获取关联字段
            Object pkValue = getJoinPkValue(field, value);
            modelMap.put(fieldName, pkValue);
        }
        return modelMap;
    }

    /**
     * 获取关联对象的主键值
     *
     * @param field 字段
     * @param value 字段值
     * @see AuditJoinPK 通过该注解申明连接对象的主键字段名称，如果没有使用AuditJoinPK注解，会尝试以对象的id或者code作为主键字段
     * 优先级： AuditJoinPK > code > id
     */
    public static Object getJoinPkValue(Field field, Object value) throws IllegalAccessException {
        Class<?> fieldClazz = field.getType();
        // 指定pk的名称
        String appointPkName = null;
        // 存在
        if (field.isAnnotationPresent(AuditJoinPK.class)) {
            AuditJoinPK auditJoinPK = field.getAnnotation(AuditJoinPK.class);
            appointPkName = auditJoinPK.value();
        }
        // 关联模型的所有字段
        Class<?> relationClazz = fieldClazz;
        List<Field> relationFields = new LinkedList<>();
        //向上循环 遍历父类，获取所有字段
        for (; relationClazz != null && relationClazz != Object.class; relationClazz = relationClazz.getSuperclass()) {
            Field[] fields = relationClazz.getDeclaredFields();
            if (fields.length > 0) {
                relationFields.addAll(Arrays.asList(fields));
            }
        }
        Object idPK = null;
        Object codePK = null;
        // 获取字段中的主键
        for (Field relationField : relationFields) {
            relationField.setAccessible(true);
            Object relationValue = relationField.get(value);
            String childFieldName = relationField.getName();
            // 判断是否是忽略字段
            if (isIgnoreFiled(relationField, relationValue)) {
                continue;
            }
            if (appointPkName != null && appointPkName.equals(childFieldName)) {
                // 如果是指定的主键，则直接返回
                return relationValue;
            }
            if (FIELD_CODE.equals(childFieldName)) {
                codePK = relationValue;
            }
            if (FIELD_ID.equals(childFieldName)) {
                idPK = relationValue;
            }
        }
        if (appointPkName != null) {
            // 如果指定了主键，但是主键在忽略字段中，则直接返回空
            return null;
        }
        if (codePK != null) {
            return codePK;
        }
        return idPK;
    }

    /**
     * 是否忽略字段
     * 忽略值为空的字段、静态字段、集合、数组
     */
    public static boolean isIgnoreFiled(Field field, Object value) {
        Class<?> fieldClazz = field.getType();
        return Objects.isNull(value) || Modifier.isStatic(field.getModifiers())
                || ClassUtils.isPrimitiveArray(fieldClazz) || ClassUtils.isPrimitiveWrapperArray(fieldClazz)
                || Collection.class.isAssignableFrom(fieldClazz);
    }

    /**
     * 通过字段名获取对应字段值
     *
     * @param fieldValues 字段值列表
     * @param fieldNames  字段名列表
     * @return 字段值
     */
    public static Object getFieldByName(Object[] fieldValues, String[] fieldNames, String fieldName) {
        if (fieldValues.length != fieldNames.length) {
            return null;
        }
        for (int i = 0; i < fieldValues.length; i++) {
            if (fieldNames[i].equals(fieldName)) {
                return fieldValues[i];
            }
        }
        return null;
    }

    /**
     * 获取模型的主键值
     *
     * @param model 模型
     * 优先级： code > id
     */
    public static String getPkValue(Object model) {
        // 模型的所有字段
        Class<?> modelClazz = model.getClass();
        List<Field> modelFields = new LinkedList<>();
        //向上循环 遍历父类，获取所有字段
        for (; modelClazz != null && modelClazz != Object.class; modelClazz = modelClazz.getSuperclass()) {
            Field[] fields = modelClazz.getDeclaredFields();
            if (fields.length > 0) {
                modelFields.addAll(Arrays.asList(fields));
            }
        }
        Object idPK = null;
        Object codePK = null;
        // 获取字段中的主键
        for (Field modelField : modelFields) {
            modelField.setAccessible(true);
            Object modelValue = null;
            try {
                modelValue = modelField.get(model);
            } catch (IllegalAccessException e) {
                logger.error("获取模型的主键值失败", e);
                return null;
            }
            String fieldName = modelField.getName();
            // 判断是否是忽略字段
            if (isIgnoreFiled(modelField, modelValue)) {
                continue;
            }
            if (FIELD_CODE.equals(fieldName)) {
                codePK = modelValue;
                break;
            }
            if (FIELD_ID.equals(fieldName)) {
                idPK = modelValue;
            }
        }
        if (codePK != null) {
            return codePK.toString();
        }
        if (idPK != null) {
            return idPK.toString();
        }
        return null;
    }
}