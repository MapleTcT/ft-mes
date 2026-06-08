package com.supcon.supfusion.theme.common.util;

import org.springframework.util.StringUtils;

public class FileTypeUtils {

    public static boolean toFileType(String type) {
        if (!StringUtils.isEmpty(type)) {
            switch (type.toUpperCase()) {
                case "PNG": {
                    return true;
                }
                case "JPEG": {
                    return true;
                }
                case "JPG": {
                    return true;
                }
                default:
                    return false;
            }
        }
        return false;
    }
}
