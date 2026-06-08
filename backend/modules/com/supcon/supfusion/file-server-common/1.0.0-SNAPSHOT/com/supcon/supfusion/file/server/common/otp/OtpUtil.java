package com.supcon.supfusion.file.server.common.otp;

import com.supcon.supfusion.file.server.common.otp.util.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.supcon.supfusion.file.server.common.otp.util.TotpUtil.generateTOTP;

@Slf4j
public class OtpUtil {

    //生成密钥存入文件名
    private static String getSecretBase32(String businessId) {
        String secretBase32 = TotpUtil.getRandomSecretBase32(64);
        Map<String, Object> codeMap = getSecretHex(secretBase32);
        //将code存入文件 + 业务id 存入文件名 用于后续校验
        createOtpCodeFile(businessId, PropertiesFileUtil.getKeyOrNull(codeMap), (String) PropertiesFileUtil.getFirstOrNull(codeMap));
        return secretBase32;
    }

    //获取密钥对应时间的6位code
    public static Map<String, Object> getSecretHex(String secretBase32) {
        String secretHex = "";
        String code = "";
        try {
            secretHex = HexEncoding.encode(Base32String.decode(secretBase32));
        } catch (Base32String.DecodingException e) {
            log.error("解码" + secretBase32 + "出错，", e);
            throw new RuntimeException("解码Base32出错");
        }
        //时间间隔多久 OTP 转换6位pw更新 单位 s
        long X = OtpConstants.TIME;
        String steps = "0";
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        long currentTimeMillis = System.currentTimeMillis();
        long currentTime = currentTimeMillis / 1000L;
        Map<String, Object> map = new HashMap<>();
        try {
            long t = currentTime / X;
            steps = Long.toHexString(t).toUpperCase();
            while (steps.length() < 16) steps = "0" + steps;
            code = generateTOTP(secretHex, steps, "6", "HmacSHA1");
            map.put(code, currentTimeMillis + "");
            return map;
        } catch (final Exception e) {
            log.error("生成动态口令出错：" + secretBase32, e);
            throw new RuntimeException("生成动态口令出错");
        }
    }

    private static void createOtpCodeFile(String businessId, String code, String currentTime) {
        String OtpFilePath = System.getProperty(OtpConstants.USER_DIR) + OtpConstants.PATH;
        File OtpFilePathFile = new File(OtpFilePath);
        if(!OtpFilePathFile.exists()){
            OtpFilePathFile.mkdir();
        }
        String path = System.getProperty(OtpConstants.USER_DIR) + OtpConstants.PATH + code + businessId + ".properties";
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("create otp code file fail:businessId ");
            }
        }
        PropertiesFileUtil.writeToProperties(currentTime, currentTime, path);
    }

    //校验服务端传过来的密钥 是否过期 没过期返回true
    public static boolean checkSecretHex(String secretBase32, String businessId) {
        Boolean isTrue = false;
        String secretHexServer = "";
        Map<String, Object> codeMap = getSecretHex(secretBase32);
        secretHexServer = PropertiesFileUtil.getKeyOrNull(codeMap);
        //从文件获取对应的 SecretHex
        String path = System.getProperty(OtpConstants.USER_DIR) + OtpConstants.PATH;
        File file = new File(path);
        if (file != null && file.listFiles().length > 0) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                if (file1.getName().equals(secretHexServer + businessId + ".properties")) {
                    isTrue = true;
                    break;
                }
            }
        }
        return isTrue;
    }

    //检验多久变化一次
    public static void main(String[] args) {
        Integer time = 80;
        String secretBase32 = getSecretBase32("xxx");
        System.out.println("secretBase32:" + secretBase32 + ",time:" + System.currentTimeMillis());
        System.out.println("1:" + getSecretHex(secretBase32));
        while (time > 0) {
            try {
                Thread.sleep(1 * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("==" + time + ":" + getSecretHex(secretBase32));
            System.out.println("==" + time + "CHECK:" + checkSecretHex(secretBase32, "xxx"));
            time--;
        }
    }

}
