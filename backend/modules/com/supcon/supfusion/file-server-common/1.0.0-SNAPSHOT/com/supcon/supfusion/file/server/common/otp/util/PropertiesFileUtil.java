package com.supcon.supfusion.file.server.common.otp.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class PropertiesFileUtil {
    /**
     * 根据以map的形式返回所有的键值对
     *
     * @param filePath 属性文件路径
     */
    public static Map readValue(String filePath) {
        Properties properties = new Properties();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));) {
            properties.load(br);
            Map map = new HashMap();
            for (String key : properties.stringPropertyNames()) {
                map.put(key, properties.getProperty(key));
            }
            return map;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * 向properties文件添加键值对 会重复
     *
     * @param moduleCode 键名
     * @param index      键值
     */
    public static void writeToProperties(String moduleCode, String index, String destFile) {
        Map map = readValue(destFile);
        if (map == null) {
            map = new HashMap();
            map.put(moduleCode, index);
        } else {
            map.put(moduleCode, index);
        }
        //1.先实例化一个Properties对象
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(destFile);
             OutputStream fos = new FileOutputStream(destFile);) {
            properties.load(fileInputStream);
            map.forEach((k, v) -> {
                properties.setProperty(k.toString(), v.toString());
            });
            properties.store(fos, "");
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 获取map中第一个key值
     *
     * @param map 数据源
     * @return
     */
    public static String getKeyOrNull(Map<String, Object> map) {
        String obj = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            obj = entry.getKey();
            if (obj != null) {
                break;
            }
        }
        return obj;
    }

    /**
     * 获取map中第一个数据值
     *
     * @param map 数据源
     * @return
     */
    public static Object getFirstOrNull(Map<String, Object> map) {
        Object obj = null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            obj = entry.getValue();
            if (obj != null) {
                break;
            }
        }
        return obj;
    }
}
