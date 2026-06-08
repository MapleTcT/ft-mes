package com.supcon.supfusion.notification.admin.service.util;

import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import org.springframework.util.StringUtils;

public class SpecialCharacterUtil {
    public static String[] convert(String chars, DbStringUtil dbStringUtil) {
        if (StringUtils.isEmpty(chars)) {
            return null;
        }
        String[] characters = chars.split(",");
        for (int i = 0; i < characters.length; i++) {
            characters[i] = dbStringUtil.getString(characters[i]);
        }
        return characters;
    }
}
