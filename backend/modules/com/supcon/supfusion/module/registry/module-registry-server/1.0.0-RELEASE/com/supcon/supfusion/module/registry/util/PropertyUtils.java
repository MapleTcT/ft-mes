/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * @author: zhuangmh
 * @date: 2020年7月13日 上午10:21:20
 */
public class PropertyUtils {
    
    private PropertyUtils() {
        throw new IllegalStateException("PropertyUtils is utility class, do not instantiate");
    }
    
    /**
     * 加载properties文件所有的key
     * @param propertiesUri properties文件路径
     * @return
     * @throws IOException
     */
    public static Set<Object> loadAllPropertyKeys(String propertiesUri) throws IOException {
        Properties properties = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream resource = loader.getResourceAsStream(propertiesUri);
        if (resource == null) {
            resource = ClassLoader.getSystemResourceAsStream(propertiesUri);
        }
        if (resource == null) {
            throw new IOException(String.format("%s not exist", propertiesUri));
        }
        properties.load(resource);
        return properties.keySet();
    }
}
