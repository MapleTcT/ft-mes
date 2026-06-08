package com.supcon.supfusion.i18n.common.until;

import java.util.UUID;

public class TokenUtil {

    public static String getToken(String moduleCode,String versionCode){
        return  moduleCode + Constants.STR_LINE + versionCode + Constants.STR_LINE + UUID.randomUUID();
    }
}
