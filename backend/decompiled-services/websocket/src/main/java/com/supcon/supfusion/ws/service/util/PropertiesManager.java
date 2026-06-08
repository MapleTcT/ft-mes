/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.supcon.supfusion.ws.service.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesManager {
    private static final Logger log = LoggerFactory.getLogger(PropertiesManager.class);
    private static final String CONFIG_FILE_NAME = "ws-config.properties";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    private static final String NACOS_GROUP = "nacos.group";
    private static Properties properties = new Properties();

    public static String getString(String key, String defaultValue) {
        String sysProperty = System.getProperty(key);
        if (null != sysProperty) {
            return sysProperty;
        }
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = properties.getProperty(key);
        return val == null ? defaultValue : Integer.parseInt(val);
    }

    public static String getNacosGroup() {
        String nacosGroup = System.getProperty(NACOS_GROUP);
        if (Objects.nonNull(nacosGroup)) {
            return nacosGroup;
        }
        return PropertiesManager.getString(NACOS_GROUP, DEFAULT_GROUP);
    }

    static {
        InputStream is = PropertiesManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
        try {
            properties.load(is);
            for (Map.Entry next : properties.entrySet()) {
                log.info("key is {},value is {}", next.getKey(), next.getValue());
            }
        }
        catch (IOException e) {
            log.error("file is error ", (Throwable)e);
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

