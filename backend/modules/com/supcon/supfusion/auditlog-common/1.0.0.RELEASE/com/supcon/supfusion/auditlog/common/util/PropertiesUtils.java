package com.supcon.supfusion.auditlog.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesUtils {
    public PropertiesUtils() {
    }

    public static String convertToString(Properties properties) throws IOException {
        StringBuffer content = new StringBuffer();
        properties.forEach((key, value) -> {
            content.append(key).append("=").append(value).append("\r\n");
        });
        return content.toString();
    }

    public static Properties parseString(String content) throws IOException {
        Properties props = new Properties();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        props.load(inputStream);
        return props;
    }
}
