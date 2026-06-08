package com.supcon.supfusion.configuration.services.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author kk.C
 * @Description: Unicode编码解码工具类
 * @Date 2021/2/3 10:14
 */
public class UnicodeUtils {

    /**
     * @param string
     * @return
     * @Title: unicodeEncode
     * @Description: unicode编码
     */
    public static String unicodeEncode(String string) {
        char[] utfBytes = string.toCharArray();
        String unicodeBytes = "";
        for (int i = 0; i < utfBytes.length; i++) {
            String hexB = Integer.toHexString(utfBytes[i]);
            if (hexB.length() <= 2) {
                hexB = "00" + hexB;
            }
            unicodeBytes = unicodeBytes + "\\u" + hexB;
        }
        return unicodeBytes;
    }

    /**
     * @return
     * @Title: unicodeDecode
     * @Description: unicode解码
     */
    public static String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
    }

    public static void main(String[] args) {
        String sourceData = "^&^*^!#^&@^*";
        String unicodeEncode = unicodeEncode(sourceData);
        System.out.println("编码结果：");
        System.out.println(unicodeEncode);//\u8fd9\u662f\u539f\u59cb\u7684\u6570\u636e\uff01\uff01\uff01

        String unicodeDecode = unicodeDecode(unicodeEncode);
        System.out.println("解码结果：");
        System.out.println(unicodeDecode);//这是原始的数据！！！
    }
}
