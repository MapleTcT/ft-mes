package com.supcon.supfusion.ws.service.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class PropertiesManager {
    private static final String CONFIG_FILE_NAME = "ws-config.properties";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    private static final String NACOS_GROUP = "nacos.group";
    private static Properties properties = new Properties();

    static {
        InputStream is = PropertiesManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
        try {
            properties.load(is);
            Iterator<Map.Entry<Object, Object>> iterator = properties.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Object> next = iterator.next();
                log.info("key is {},value is {}", next.getKey(), next.getValue());
            }
        } catch (IOException e) {
            log.error("file is error ", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getString(String key, String defaultValue) {
        String sysProperty = System.getProperty(key);
        if(null != sysProperty){
            return sysProperty;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = properties.getProperty(key);
        return (val == null) ? defaultValue : Integer.parseInt(val);
    }

    /**
     * 获取nacos所在组
     */
    public static String getNacosGroup() {
        // 从命令参数获取
        String nacosGroup = System.getProperty(NACOS_GROUP);
        if (Objects.nonNull(nacosGroup)) {
            return nacosGroup;
        }
        // 从配置文件获取
        return getString(NACOS_GROUP, DEFAULT_GROUP);
    }

}
