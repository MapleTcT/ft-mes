package com.supcon.supfusion.auth.common.utils;

import com.supcon.supfusion.auth.common.config.BCryptConfig;

public class BCryptUtil {
    private static BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    /**
     * 是否开启国密
     */
    private static boolean enableNationalEncry;

    public static void setConfigInfo(BCryptConfig bCryptConfig) {
        BCryptUtil.enableNationalEncry = bCryptConfig.isEnableNationalEncry();
    }

    public static boolean getEnableEncry(){
        return enableNationalEncry;
    }

    public static String encode(String rawPassword) {
        if (getEnableEncry()){
            String encrypt = SM3Utils.encrypt(rawPassword);
            return bCryptPasswordEncoder.encode(encrypt);
        }else {
            return bCryptPasswordEncoder.encode(rawPassword);
        }
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (getEnableEncry()){
            return bCryptPasswordEncoder.matches(SM3Utils.encrypt(rawPassword), encodedPassword);
        }else {
            return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
        }
    }

    public static void main(String[] args) {
        String encode = BCryptUtil.encode("Supcon1304@");
        System.out.println(encode);
        System.out.println(BCryptUtil.matches("Supcon1304@", encode));
    }

}
