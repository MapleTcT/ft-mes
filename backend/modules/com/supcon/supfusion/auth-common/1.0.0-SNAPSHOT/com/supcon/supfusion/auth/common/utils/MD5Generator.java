package com.supcon.supfusion.auth.common.utils;

import java.security.MessageDigest;
import java.util.UUID;

public class MD5Generator {
    private static final char[] hexCode = "0123456789abcdef".toCharArray();

    private static MD5Generator instance = new MD5Generator();

    public static MD5Generator getInstance(){
        return instance;
    }

    private MD5Generator() {
    }

    public String generateValue() throws Exception {
        return generateValue(UUID.randomUUID().toString());
    }

    private  String toHexString(byte[] data) {
        if (data == null) {
            return null;
        } else {
            StringBuilder r = new StringBuilder(data.length * 2);
            byte[] arr$ = data;
            int len$ = data.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                byte b = arr$[i$];
                r.append(hexCode[b >> 4 & 15]);
                r.append(hexCode[b & 15]);
            }

            return r.toString();
        }
    }

    public   String generateValue(String param) throws Exception {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(param.getBytes());
            byte[] messageDigest = algorithm.digest();
            return toHexString(messageDigest);
        } catch (Exception var4) {
            throw new Exception("OAuth Token cannot be generated.", var4);
        }
    }
}
