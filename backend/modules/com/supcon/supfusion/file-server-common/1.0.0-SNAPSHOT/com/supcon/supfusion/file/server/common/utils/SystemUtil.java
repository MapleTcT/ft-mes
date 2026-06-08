package com.supcon.supfusion.file.server.common.utils;

import com.supcon.supfusion.file.server.common.constants.Constants;

public class SystemUtil {
    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("linux") >= 0) {
            return Constants.LINUX;
        }
        return Constants.WINDOWS;
    }

}
