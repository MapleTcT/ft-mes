/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.lang.Nullable
 *  org.springframework.util.StringUtils
 */
package com.supcon.supfusion.framework.cloud.common.util;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

public class StringExUtil
extends StringUtils {
    public static boolean isBlank(@Nullable CharSequence cs) {
        return !StringExUtil.hasText((CharSequence)cs);
    }

    public static boolean isNotBlank(CharSequence cs) {
        return StringExUtil.hasText((CharSequence)cs);
    }

    public static String[] toStrArray(String str) {
        return StringExUtil.toStrArray(",", str);
    }

    public static String[] toStrArray(String split, String str) {
        if (StringExUtil.isBlank(str)) {
            return new String[0];
        }
        return str.split(split);
    }

    public static String humpToUnderline(String para) {
        para = StringExUtil.lowerFirst(para);
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;
        for (int i = 0; i < para.length(); ++i) {
            if (!Character.isUpperCase(para.charAt(i))) continue;
            sb.insert(i + temp, "_");
            ++temp;
        }
        return sb.toString().toLowerCase();
    }

    public static String humpToUnderlineUpperCase(String para) {
        para = StringExUtil.lowerFirst(para);
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;
        for (int i = 0; i < para.length(); ++i) {
            if (!Character.isUpperCase(para.charAt(i))) continue;
            sb.insert(i + temp, "_");
            ++temp;
        }
        return sb.toString().toUpperCase();
    }

    public static String lowerFirst(String str) {
        char firstChar = str.charAt(0);
        if (firstChar >= 'A' && firstChar <= 'Z') {
            char[] arr = str.toCharArray();
            arr[0] = (char)(arr[0] + 32);
            return new String(arr);
        }
        return str;
    }

    public static StringBuilder appendBuilder(StringBuilder sb, CharSequence ... strs) {
        for (CharSequence str : strs) {
            sb.append(str);
        }
        return sb;
    }

    public static String removeSuffix(CharSequence str, CharSequence suffix) {
        if (StringExUtil.isEmpty((Object)str) || StringExUtil.isEmpty((Object)suffix)) {
            return "";
        }
        String str2 = str.toString();
        if (str2.endsWith(suffix.toString())) {
            return StringExUtil.subPre(str2, str2.length() - suffix.length());
        }
        return str2;
    }

    public static String subPre(CharSequence string, int toIndex) {
        return StringExUtil.sub(string, 0, toIndex);
    }

    public static String sub(CharSequence str, int fromIndex, int toIndex) {
        if (StringExUtil.isEmpty((Object)str)) {
            return "";
        }
        int len = str.length();
        if (fromIndex < 0) {
            if ((fromIndex = len + fromIndex) < 0) {
                fromIndex = 0;
            }
        } else if (fromIndex > len) {
            fromIndex = len;
        }
        if (toIndex < 0) {
            if ((toIndex = len + toIndex) < 0) {
                toIndex = len;
            }
        } else if (toIndex > len) {
            toIndex = len;
        }
        if (toIndex < fromIndex) {
            int tmp = fromIndex;
            fromIndex = toIndex;
            toIndex = tmp;
        }
        if (fromIndex == toIndex) {
            return "";
        }
        return str.toString().substring(fromIndex, toIndex);
    }
}

