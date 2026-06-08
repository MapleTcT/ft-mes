package com.supcon.supfusion.i18n.common.until;

import java.util.*;

public class ListUtils {

    public static Map<String, String> mapStringToMap(String str) {
        str = str.substring(1, str.length() - 1);
        String[] strs = str.split(",");
        Map<String, String> map = new HashMap<String, String>();
        for (String string : strs) {
            String key = string.split("=")[0];
            String value = string.split("=")[1];
            map.put(key.replace(Constants.STR_SPACE,Constants.STR_NO_SPACE), value.replace(Constants.STR_SPACE,Constants.STR_NO_SPACE));
        }
        return map;
    }

    public static List<String> listStringToList(String str) {
        str = str.substring(1, str.length() - 1).replace(Constants.STR_SPACE,Constants.STR_NO_SPACE);
        String[] strs = str.split(",");
        List<String> list = new ArrayList<>(Arrays.asList(strs));
        return list;
    }
}
