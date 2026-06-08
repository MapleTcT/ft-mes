package com.supcon.supfusion.authkeycloak.bcrypt;

import com.supcon.supfusion.authkeycloak.configure.PropertiesConfigure;
import com.supcon.supfusion.authkeycloak.util.SM3Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BCryptUtil {
    private static BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    /**
     * 是否开启国密
     */
    private static boolean enableNationalEncry;

    static {
        String enable = PropertiesConfigure.getProperties().getProperty("nationalEncry-enable", "false");
        enableNationalEncry = Boolean.parseBoolean(enable);
    }

    public static String encode(String rawPassword) {
        if (enableNationalEncry){
            String encrypt = SM3Utils.encrypt(rawPassword);
            return bCryptPasswordEncoder.encode(encrypt);
        }else {
            return bCryptPasswordEncoder.encode(rawPassword);
        }
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        log.info("原密码：" + rawPassword + " 加密密码：" + encodedPassword + " 是否开启SM3：" + enableNationalEncry);
        if (enableNationalEncry){
            return bCryptPasswordEncoder.matches(SM3Utils.encrypt(rawPassword), encodedPassword);
        }else {
            return bCryptPasswordEncoder.matches(rawPassword, encodedPassword);
        }
    }

    public static void main(String[] args) {
        String encode = BCryptUtil.encode("Supcon1304@");
        System.out.println(encode);
        //System.out.println(BCryptUtil.matches("Supcon1304@", encode));

        String resCode = "$2a$10$bST.JhOKIp.xwdXe9uxiqeFqUCgSYhzBleu/IPct9ikzAMniECr7W";
        System.out.println(BCryptUtil.matches("Supcon1304@@", resCode));
    }
}
