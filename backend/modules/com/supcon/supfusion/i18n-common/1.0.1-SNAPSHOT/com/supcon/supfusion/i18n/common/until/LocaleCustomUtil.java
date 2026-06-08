package com.supcon.supfusion.i18n.common.until;

public class LocaleCustomUtil {
    public static String localeChange(String language) {
        String  languageStrP = language.substring(0, 2);
        String languageStrS = language.substring(3, 5);
        String languageChange = languageStrP + Constants.STR_LINE + languageStrS.toUpperCase();
        return languageChange;
    }
}
