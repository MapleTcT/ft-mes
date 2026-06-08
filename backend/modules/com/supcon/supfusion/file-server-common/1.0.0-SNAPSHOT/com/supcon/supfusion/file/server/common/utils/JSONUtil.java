package com.supcon.supfusion.file.server.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

public class JSONUtil {
    public static <T> String toJSONString(T t) {
        return JSON.toJSONString(t);
    }

    public static <T> JSONObject toJSONObject(T t) {
        return (JSONObject) JSON.toJSON(t);
    }

    public static <T> T parseToObject(String s, Class<T> clazz) {
        return JSON.parseObject(s, clazz);
    }

    public static <T> T parseToObject(String s, TypeReference<T> typeReference) {
        return JSON.parseObject(s, typeReference);
    }
}
