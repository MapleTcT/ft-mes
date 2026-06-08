/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.EcEnv;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

@Data
@Slf4j
@Component
public class PropertyHolder implements InitializingBean {

    private static PropertyHolder propertyHolder;

    @Autowired
    private Environment environment;

    private String workspacePath;
    private String generatePath;
    private String projPath;
    private String viewPath;
    private String staticPath;
    private String misStaticPath;
    private String customFilePath;
    private String customL10nPath;
    private String l10nPath;
    private String scriptsPath;
    private String repositoryPath;

    public static String profile = "prod";

    @Override
    public void afterPropertiesSet() {
        this.workspacePath = environment.getProperty("configuration-services.workspace");
        File workspace = new File(workspacePath);
        if (!workspace.exists() && !workspace.mkdirs()) {
            throw new Error(
                    "Can not create the workspace directory. Please check the user running the BAP server has the permission to operate the directory "
                            + workspacePath);
        }
        this.generatePath = workspacePath + File.separator + "generate";
        this.projPath =workspacePath + File.separator + "proj";
        this.viewPath = workspacePath + File.separator + "template";
        this.staticPath = workspacePath + File.separator + "static";
        this.customFilePath=workspacePath + File.separator + "custom_template";
        this.customL10nPath = workspacePath + File.separator + "custom" + File.separator + "l10n";
        this.l10nPath = workspacePath + File.separator + "l10n";
        this.scriptsPath = workspacePath + File.separator + "scripts";
        this.misStaticPath = workspacePath + File.separator + "bap-static";
        String active = "dev";
        if (!StringUtils.isEmpty(active)) {
            profile = active;
        }
        this.repositoryPath = environment.getProperty("maven.repository.path");
        if (StringUtils.isEmpty(repositoryPath)) {
            this.repositoryPath = workspacePath + File.separator + "../assembly/repository/maven";
        }
    }

    @PostConstruct
    public void init() {
        propertyHolder = this;
    }

    public static PropertyHolder get() {
        return propertyHolder;
    }

    public static EcEnv getEcEnv() {
        return EcEnv.product;
    }

    public static boolean isDev() {
        return "dev".equals(profile);
    }

    public static boolean isProduct(){
        return !isDev();
    }
    /**
     * 工程环境(含工程开发productDev)
     * @return
     */
    public static boolean isProject() {
        return !isDev();
    }
    public static boolean isDebugMode() {
        return "dev".equals(System.getProperty("orchid.env"));
    }

}