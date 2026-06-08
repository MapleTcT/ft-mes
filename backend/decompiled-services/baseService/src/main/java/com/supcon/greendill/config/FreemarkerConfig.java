/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  freemarker.template.Configuration
 *  freemarker.template.TemplateException
 *  freemarker.template.TemplateModel
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.ui.freemarker.FreeMarkerConfigurationFactory
 *  org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer
 */
package com.supcon.greendill.config;

import com.supcon.greendill.config.method.GetUserFontPropertyMethodModel;
import com.supcon.greendill.config.method.I18nMethodModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import java.io.IOException;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@Configuration
public class FreemarkerConfig {
    @Autowired
    private FreeMarkerProperties properties;

    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() throws IOException, TemplateException {
        FreeMarkerConfigurer config = new FreeMarkerConfigurer();
        this.applyProperties((FreeMarkerConfigurationFactory)config);
        freemarker.template.Configuration configuration = config.createConfiguration();
        configuration.setSharedVariable("getText", (TemplateModel)new I18nMethodModel());
        configuration.setSharedVariable("getUserFontProperty", (TemplateModel)new GetUserFontPropertyMethodModel());
        config.setConfiguration(configuration);
        return config;
    }

    private void applyProperties(FreeMarkerConfigurationFactory factory) {
        factory.setTemplateLoaderPaths(this.properties.getTemplateLoaderPath());
        factory.setPreferFileSystemAccess(this.properties.isPreferFileSystemAccess());
        factory.setDefaultEncoding(this.properties.getCharsetName());
        Properties settings = new Properties();
        settings.putAll(this.properties.getSettings());
        factory.setFreemarkerSettings(settings);
    }
}

