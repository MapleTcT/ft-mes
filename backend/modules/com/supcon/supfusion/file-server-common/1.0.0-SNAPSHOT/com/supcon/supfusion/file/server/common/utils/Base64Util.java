package com.supcon.supfusion.file.server.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * @author Miracle Luna
 * @version 1.0
 * @date 2019/7/3 18:55
 */
@Slf4j
public class Base64Util {

    final static Base64.Encoder encoder = Base64.getEncoder();
    final static Base64.Decoder decoder = Base64.getDecoder();

    /**
     * 给字符串加密
     *
     * @param text
     * @return
     */
    public static String encode(String text) {
        String encodedText = null;
        try {
            byte[] textByte = new byte[0];
            textByte = text.getBytes("UTF-8");
            encodedText = encoder.encodeToString(textByte);
        } catch (UnsupportedEncodingException e) {
            log.error("base64加密报错,text:{}", text);
        }
        return encodedText;
    }

    /**
     * 将加密后的字符串进行解密
     *
     * @param encodedText
     * @return
     */
    public static String decode(String encodedText) {
        String text = null;
        try {
            text = new String(decoder.decode(encodedText), "UTF-8");
        } catch (Exception e) {
            log.error("base64解密报错,encodedText:{}", encodedText);
        }
        return text;
    }
}