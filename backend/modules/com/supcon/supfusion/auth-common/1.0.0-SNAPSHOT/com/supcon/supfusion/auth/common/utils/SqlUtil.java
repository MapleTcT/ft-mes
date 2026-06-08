package com.supcon.supfusion.auth.common.utils;

/**
 * @author caokele
 */
public class SqlUtil {
    /**
     * 特殊字符转义
     */
    public static String escapeChar(String param) {
        return param.replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("_", "\\\\_")
                .replaceAll("%", "\\\\%");
    }
}
