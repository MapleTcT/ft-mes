package com.supcon.supfusion.file.server.common.otp.util;

public class OtpConstants {
    public static Integer TIME = 180;//设定 同一个  secretBase32 经过getSecretHex(secretBase32) 算出验证码 180s内相同
    public static String USER_DIR = "user.dir";//
    public static String PATH = "/OTP/";//

}
