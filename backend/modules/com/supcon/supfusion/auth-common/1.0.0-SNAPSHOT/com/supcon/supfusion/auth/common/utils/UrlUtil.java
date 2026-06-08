package com.supcon.supfusion.auth.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author caokele
 */
public class UrlUtil {

    public static String getQueryParam(String url, String name) {
        if (url == null) {
            return null;
        }
        url += "&";
        String pattern = "(\\?|&){1}#{0,1}" + name + "=[a-zA-Z0-9\\-]*(&{1})";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(url);
        if (matcher.find()) {
            return matcher.group(0).split("=")[1].replace("&", "");
        } else {
            return null;
        }
    }
}
