package com.supcon.supfusion.authkeycloak.configure;

import java.util.Properties;

public class PropertiesConfigure {
    private PropertiesConfigure() {

    }

    private static Properties properties = new Properties();

    public static Properties getProperties() {
        return properties;
    }

    public static void setProperties(Properties properties) {
        PropertiesConfigure.properties = properties;
    }
}
