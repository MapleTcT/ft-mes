package com.supcon.supfusion.notification.engine.common;

public class NumberUtil {
    public static boolean validString(String s) {
        if (s == null || "".equals(s) || !s.matches("[0-9]+")) {
            return false;
        }
        return true;
    }
}
