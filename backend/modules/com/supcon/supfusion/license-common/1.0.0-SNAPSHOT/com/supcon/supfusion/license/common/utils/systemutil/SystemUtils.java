package com.supcon.supfusion.license.common.utils.systemutil;

/**
 * 系统工具类
 */
public class SystemUtils {

    public static SupportOS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("linux") >= 0) {
            return SupportOS.LINUX;
        }
        return SupportOS.WINDOWS;
    }
}
