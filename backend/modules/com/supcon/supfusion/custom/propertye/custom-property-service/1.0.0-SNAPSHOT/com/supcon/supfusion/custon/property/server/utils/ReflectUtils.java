package com.supcon.supfusion.custon.property.server.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhang yafei
 */
public class ReflectUtils {


    public static Map<String, Object> getDeclaredFieldValues(Object obj) throws IllegalArgumentException, IllegalAccessException {
        Map<String, Object> retMap = new HashMap();
        Class clazz = obj.getClass();
        Field[] arr$ = clazz.getDeclaredFields();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            Field field = arr$[i$];
            if (field.isAccessible()) {
                retMap.put(field.getName(), field.get(obj));
            }
        }

        return retMap;
    }
}
