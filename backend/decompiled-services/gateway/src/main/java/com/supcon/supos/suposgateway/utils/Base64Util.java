/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supos.suposgateway.utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Base64Util {
    private static final Logger log = LoggerFactory.getLogger(Base64Util.class);
    static final Base64.Encoder encoder = Base64.getEncoder();
    static final Base64.Decoder decoder = Base64.getDecoder();

    public static String encode(String text) {
        String encodedText = null;
        try {
            byte[] textByte = new byte[]{};
            textByte = text.getBytes("UTF-8");
            encodedText = encoder.encodeToString(textByte);
        }
        catch (UnsupportedEncodingException e) {
            log.error("base64\u52a0\u5bc6\u62a5\u9519,text:{}", (Object)text);
        }
        return encodedText;
    }

    public static String decode(String encodedText) {
        String text = null;
        try {
            text = new String(decoder.decode(encodedText), "UTF-8");
        }
        catch (Exception e) {
            log.error("base64\u89e3\u5bc6\u62a5\u9519,encodedText:{}", (Object)encodedText);
        }
        return text;
    }
}

