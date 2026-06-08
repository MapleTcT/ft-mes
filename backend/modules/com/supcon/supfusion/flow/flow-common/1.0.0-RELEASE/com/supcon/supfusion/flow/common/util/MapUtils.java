/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author: zhuangmh
 * @date: 2020年6月2日 上午10:04:45
 */
public class MapUtils {
    
    private MapUtils() {
        throw new IllegalStateException("MapUtils is utility class, do not instantiate");
    }
    
    /**
     * 将JSON解析为map, 全部将对象转化为字符串存储, 防止序列化异常
     * @param formJson
     * @return
     */
    public static Map<String, Object> jsonToMap(String formJson) {
        if (formJson == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        // 最多解析三层嵌套
        Map<String, Object> formMap = new Gson().fromJson(formJson, type);
        // 初始化分配64防止频繁扩容
        Map<String, Object> newFormMap = new HashMap<>(64);
        for (Map.Entry<String, Object> entry : formMap.entrySet()) {
            if (entry.getValue() instanceof List) {
                List<?> list = (List)entry.getValue();
                StringBuilder sb = new StringBuilder();
                for (Object val : list) {
                    sb.append(",").append(val.toString());
                }
                if (!list.isEmpty()) {
                    newFormMap.put(entry.getKey(), sb.substring(1));
                }
            } else {
                newFormMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }
        return newFormMap;
    }
    
    /**
     * 将层级结构的数据全部扁平化， 例：
     * {"data":{"p1":"f1", "p2":"f2"}}" 扁平化之后  {"data.p1":"f1", "data.p2":"f2"}
     * @param formJson json字符串
     * @return 
     */
    public static Map<String, Object> jsonFlatMap(String formJson) {
        if (formJson == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        // 最多解析三层嵌套
        Map<String, Object> formMap = new Gson().fromJson(formJson, type);
        // 初始化分配64防止频繁扩容
        Map<String, Object> newFormMap = new HashMap<>(64);
        recusiveFlatMap("", formMap, newFormMap);
        return newFormMap;
    }
    
    private static void recusiveFlatMap(String parentKey, Map<String, Object> map, Map<String, Object> newContainer) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                recusiveFlatMap(parentKey + entry.getKey() + ".", (Map)value, newContainer);
            } else {
                newContainer.put(parentKey + entry.getKey(), value);
            }
        }
    }
}
