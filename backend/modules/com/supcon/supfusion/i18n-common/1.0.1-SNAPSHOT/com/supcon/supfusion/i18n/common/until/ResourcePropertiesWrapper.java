package com.supcon.supfusion.i18n.common.until;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  18:34 2020/6/22
 * @Modified:
 */
@Slf4j
public class ResourcePropertiesWrapper {

    private ResourcePropertiesWrapper() {
        throw new IllegalStateException("ResourcePropertiesWrapper class");
    }

    /**
     * 根据以map的形式返回所有的键值对
     *
     * @param filePath 属性文件路径
     */
    public static Map<String, String> readValue(String filePath) {
        Properties properties = new Properties();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));) {
            properties.load(br);
            Map<String, String> map = new HashMap<>();
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
     * 如果该主键已经存在，删除该键值对；
     * 如果该主键不存在 则不用管
     *
     * @param filePath 文件的绝对路径
     * @param key      键名
     */
    public static void removeProperty(String filePath, String key) {
        Map<String, String> map = readValue(filePath);
        map.remove(key);
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(new File(filePath));
             OutputStream outputStream = new FileOutputStream(new File(filePath));) {
            //properties.load(inputStream);
            properties.load(new InputStreamReader(inputStream, "utf-8"));
            map.forEach((k,v)->{
                properties.setProperty(k.toString(),v.toString());
            });
            //properties.store(outputStream, Constants.STR_NO_SPACE);
            properties.store(new OutputStreamWriter(outputStream, "utf-8"), Constants.STR_NO_SPACE);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    /**
     * 向properties文件添加键值对 会重复
     *
     * @param moduleCode 键名
     * @param index      键值
     */
    public static void writeToProperties(String moduleCode, String index, String destFile) {
        Map<String, String> map = readValue(destFile);
        map.put(moduleCode,index);
        //1.先实例化一个Properties对象
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(destFile);
             OutputStream fos = new FileOutputStream(destFile);) {
            //properties.load(fileInputStream);
            properties.load(new InputStreamReader(fileInputStream, "utf-8"));
            map.forEach((k,v)->{
                properties.setProperty(k.toString(),v.toString());
            });
            //properties.store(fos, Constants.STR_NO_SPACE);
            properties.store(new OutputStreamWriter(fos, "utf-8"), Constants.STR_NO_SPACE);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    /**
     * 更新properties文件的键值对
     * 如果该主键已经存在，更新该主键的值；
     * 如果该主键不存在，则插件一对键值。
     *
     * @param keyname  键名
     * @param keyvalue 键值
     */
    public static void updatePropertiesFile(String keyname, String keyvalue, String profilepath) {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(profilepath);
             OutputStream fos = new FileOutputStream(profilepath);) {
            //properties.load(fileInputStream);
            properties.load(new InputStreamReader(fileInputStream, "utf-8"));
            properties.setProperty(keyname, keyvalue);
            //properties.store(fos, "Update '" + keyname + "' value");
            properties.store(new OutputStreamWriter(fos, "utf-8"), "Update '" + keyname + "' value");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 向properties文件添加键值对 会重复
     *
     * @param map      键值对map
     * @param destFile 文件路径
     */
    public static void writeToPropertiesByMap(Map<String, String> map, String destFile) {
        if (map != null && map.size() > 0) {
            Properties properties = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(destFile);
                 OutputStream fos = new FileOutputStream(destFile);) {
                //properties.load(fileInputStream);
                properties.load(new InputStreamReader(fileInputStream, "utf-8"));
                map.forEach((k, v) -> {
                    properties.setProperty(k, v);
                });
                //properties.store(fos, Constants.STR_NO_SPACE);
                properties.store(new OutputStreamWriter(fos, "utf-8"), Constants.STR_NO_SPACE);
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

//    public static  void  main(String[] args){
//        Map map = new HashMap();
//        map = readValue("D:\\WorkSpace\\develop-szq-supfusion\\supfusion-i18n-service\\i18n-bootstrap\\src\\main\\resources\\i18nResource\\sys\\sys_zh_CN.properties");
//        writeToPropertiesByMap(map,"D:\\WorkSpace\\develop-szq-supfusion\\supfusion-i18n-service\\i18n-bootstrap\\src\\main\\resources\\package_zh_CN.properties");
//        Map map1 = new HashMap();
//        map1 = readValue("D:\\WorkSpace\\develop-szq-supfusion\\supfusion-i18n-service\\i18n-bootstrap\\src\\main\\resources\\i18nResource\\sys\\sys_en_US.properties");
//        writeToPropertiesByMap(map1,"D:\\WorkSpace\\develop-szq-supfusion\\supfusion-i18n-service\\i18n-bootstrap\\src\\main\\resources\\package_en_US.properties");
//        Map map2 = new HashMap();
//        map2 = readValue("D:\\WorkSpace\\develop-szq-supfusion\\supfusion-i18n-service\\i18n-bootstrap\\src\\main\\resources\\i18nResource\\sys\\sys_zh_HK.properties");
//        writeToPropertiesByMap(map2,"D:\\WorkSpace\\develop-szq-supfusion\\supfusion-i18n-service\\i18n-bootstrap\\src\\main\\resources\\package_zh_TW.properties");
//    }
}
