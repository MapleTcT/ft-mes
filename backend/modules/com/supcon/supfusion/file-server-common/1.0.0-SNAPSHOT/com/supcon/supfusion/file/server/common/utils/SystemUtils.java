package com.supcon.supfusion.file.server.common.utils;

/**
 * 系统工具类
 */
public class SystemUtils {

    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("linux") >= 0) {
            return "LINUX";
        }
        return "WINDOWS";
    }
}
