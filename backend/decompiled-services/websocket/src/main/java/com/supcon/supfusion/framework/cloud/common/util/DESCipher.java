/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.util.Base64Utils
 */
package com.supcon.supfusion.framework.cloud.common.util;

import com.supcon.supfusion.framework.cloud.common.supports.Charsets;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.springframework.util.Base64Utils;

public final class DESCipher {
    private static final String ALGORITHM = "PBEWithMD5AndDES";
    public static final int DEFAULT_ITERATIONS = 1000;
    public static final byte[] DEFAULT_SALT = new byte[]{115, 117, 112, 99, 111, 110, 108, 122};
    public static final String DEFAULT_PASSWORD = "vcnP#Pa#Hrba@D8p";

    public String encrypt(String src, String password, int iterations, byte[] salt) throws Exception {
        SecretKey key = this.getKey(password);
        PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, iterations);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(1, (Key)key, parameterSpec);
        byte[] data = cipher.doFinal(src.getBytes(Charsets.UTF_8));
        return Base64Utils.encodeToString((byte[])data);
    }

    public String decrypt(String encrypted, String password, int iterations, byte[] salt) throws Exception {
        byte[] src = Base64Utils.decodeFromString((String)encrypted);
        SecretKey key = this.getKey(password);
        PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, iterations);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(2, (Key)key, parameterSpec);
        byte[] data = cipher.doFinal(src);
        return new String(data);
    }

    private SecretKey getKey(String password) throws Exception {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
        SecretKey key = keyFactory.generateSecret(keySpec);
        return key;
    }

    public static void main(String[] args) throws Exception {
        String src = "123456";
        DESCipher cipher = new DESCipher();
        String e = cipher.encrypt(src, DEFAULT_PASSWORD, 1000, DEFAULT_SALT);
        System.out.println(e);
        String d = cipher.decrypt(e, DEFAULT_PASSWORD, 1000, DEFAULT_SALT);
        System.out.println(d);
    }
}

