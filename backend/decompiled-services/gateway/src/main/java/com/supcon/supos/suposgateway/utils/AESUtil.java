/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Base64
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supos.suposgateway.utils;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AESUtil {
    private static final Logger log = LoggerFactory.getLogger(AESUtil.class);
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String DEFAULT_CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding";

    public static String encrypt(String content, String key) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(1, new SecretKeySpec(key.getBytes(), KEY_ALGORITHM));
            byte[] result = cipher.doFinal(byteContent);
            return Base64.encodeBase64String((byte[])result);
        }
        catch (Exception var5) {
            log.error("AES encrypt error ", (Throwable)var5);
            return null;
        }
    }

    public static String decrypt(String content, String key) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(2, new SecretKeySpec(key.getBytes(), KEY_ALGORITHM));
            byte[] result = cipher.doFinal(Base64.decodeBase64((String)content));
            return new String(result, "utf-8");
        }
        catch (Exception var4) {
            log.error("AES decrypt error ", (Throwable)var4);
            return null;
        }
    }

    private static SecretKeySpec getSecretKey(String key) {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGenerator.init(128, new SecureRandom(key.getBytes()));
            SecretKey secretKey = keyGenerator.generateKey();
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
        }
        catch (NoSuchAlgorithmException var3) {
            log.error("AES getSecretKey error ", (Throwable)var3);
            return null;
        }
    }

    public static String encryptCBC(String content, String key, String ivParameter) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM_CBC);
            byte[] byteContent = content.getBytes("utf-8");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivParameter.getBytes());
            cipher.init(1, (Key)new SecretKeySpec(key.getBytes(), KEY_ALGORITHM), ivParameterSpec);
            byte[] result = cipher.doFinal(byteContent);
            return Base64.encodeBase64String((byte[])result);
        }
        catch (Exception var7) {
            log.error("AES encrypt error ", (Throwable)var7);
            return null;
        }
    }

    public static String decryptCBC(String content, String key, String ivParameter) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM_CBC);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(ivParameter.getBytes());
            cipher.init(2, (Key)new SecretKeySpec(key.getBytes(), KEY_ALGORITHM), ivParameterSpec);
            byte[] result = cipher.doFinal(Base64.decodeBase64((String)content));
            return new String(result, "utf-8");
        }
        catch (Exception var6) {
            log.error("AES decrypt error ", (Throwable)var6);
            return null;
        }
    }
}

