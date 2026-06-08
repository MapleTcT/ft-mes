package com.supcon.supfusion.auth.common.utils;


import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class AESUtils {
    /**
     * 密钥算法
     */
    private static final String ALGORITHM = "AES";
    /**
     * 加解密算法/工作模式/填充方式
     */
    private static final String ALGORITHM_STR = "AES/ECB/PKCS5Padding";


    /**
     * AES加密
     * @param data
     * @return
     * @throws Exception
     */
    public static String encryptData(String data,String secretKey)  {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_STR); // 创建密码器
            MessageDigest md5 = MessageDigest.getInstance("md5");
            // 准备要加密的数据
            byte[] b = secretKey.getBytes();
            // 加密
            byte[] digest = md5.digest(b);
            SecretKeySpec  key = new SecretKeySpec(digest, ALGORITHM);

            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        }catch (Exception e){
            return null;
        }

    }

    /**
     * AES解密
     * @param base64Data
     * @return
     * @throws Exception
     */
    @SneakyThrows
    public static String decryptData(String base64Data,String secretKey) {
        Cipher cipher = Cipher.getInstance(ALGORITHM_STR);
        MessageDigest md5 = MessageDigest.getInstance("md5");
        // 准备要加密的数据
        byte[] b = secretKey.getBytes();
        // 加密
        byte[] digest = md5.digest(b);
        SecretKeySpec  key = new SecretKeySpec(digest, ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.getDecoder().decode(base64Data)));
    }

    /**
     * hex字符串 转 byte数组
     * @param s
     * @return
     */
    private static byte[] hex2byte(String s) {
        if (s.length() % 2 == 0) {
            return hex2byte (s.getBytes(), 0, s.length() >> 1);
        } else {
            return hex2byte("0"+s);
        }
    }

    private static byte[] hex2byte (byte[] b, int offset, int len) {
        byte[] d = new byte[len];
        for (int i=0; i<len*2; i++) {
            int shift = i%2 == 1 ? 0 : 4;
            d[i>>1] |= Character.digit((char) b[offset+i], 16) << shift;
        }
        return d;
    }
}
