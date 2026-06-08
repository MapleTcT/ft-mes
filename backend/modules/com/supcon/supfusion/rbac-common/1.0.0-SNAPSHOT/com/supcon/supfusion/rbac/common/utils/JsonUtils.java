package com.supcon.supfusion.rbac.common.utils;

import flexjson.JSONSerializer;
import org.springframework.util.StringUtils;

import java.util.List;

public class JsonUtils {

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
}
