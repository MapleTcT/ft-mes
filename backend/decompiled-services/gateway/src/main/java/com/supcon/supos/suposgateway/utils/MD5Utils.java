/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supos.suposgateway.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
    public static String stringToMD5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(plainText.getBytes());
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("\u6ca1\u6709\u8fd9\u4e2amd5\u7b97\u6cd5\uff01");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); ++i) {
            md5code = "0" + md5code;
        }
        return md5code;
    }
}

