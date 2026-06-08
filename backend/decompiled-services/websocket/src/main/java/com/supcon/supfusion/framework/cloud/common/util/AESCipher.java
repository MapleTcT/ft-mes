/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.util.StringUtils
 */
package com.supcon.supfusion.framework.cloud.common.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

public final class AESCipher {
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    public static String encrypt(String src, String key, String iv) throws Exception {
        if (StringUtils.isEmpty((Object)src) || StringUtils.isEmpty((Object)key)) {
            return null;
        }
        Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, KEY_ALGORITHM);
        IvParameterSpec ivp = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(1, (Key)skeySpec, ivp);
        byte[] encrypted = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
        return AESCipher.byte2HexStr(encrypted);
    }

    public static String decrypt(String src, String key, String iv) throws Exception {
        if (StringUtils.isEmpty((Object)src) || StringUtils.isEmpty((Object)key)) {
            return null;
        }
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, KEY_ALGORITHM);
        Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
        IvParameterSpec ivp = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(2, (Key)skeySpec, ivp);
        byte[] encrypted = AESCipher.hexStr2Byte(src);
        byte[] originalPassByte = cipher.doFinal(encrypted);
        return new String(originalPassByte, StandardCharsets.UTF_8);
    }

    private static String byte2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; ++i) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    private static byte[] hexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; ++i) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte)(high * 16 + low);
        }
        return result;
    }
}

