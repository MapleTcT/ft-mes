package com.supcon.supfusion.notification.apiserver.common.utils;

import org.springframework.util.StringUtils;

public class NumberUtil {
    public static boolean validString(String s) {
        if (StringUtils.isEmpty(s) || !s.matches("[0-9]+")) {
            return false;
        }
        return true;
    }
}
