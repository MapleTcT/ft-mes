/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.scheduling.annotation.EnableScheduling
 *  org.springframework.scheduling.annotation.Scheduled
 */
package com.supcon.orchid.entityconf.configuration;

import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class TimedTask {
    private static final Logger log = LoggerFactory.getLogger(TimedTask.class);
    @Value(value="${supos.app.path}")
    private String appPath;
    private int hour = 3600000;

    @Scheduled(cron="0 0 1 * * ?")
    private void deleteAppFile() {
        try {
            log.info("\u5f00\u59cb\u6267\u884c\u5220\u9664APP\u5b89\u88c5\u5305");
            if (StringUtils.isBlank((CharSequence)this.appPath)) {
                return;
            }
            File file = new File(this.appPath);
            this.deleteFile(file);
        }
        catch (Exception e) {
            log.error("\u5b9a\u65f6\u5220\u9664APP\u5b89\u88c5\u5305\u6587\u4ef6\u5931\u8d25{}", (Object)e.getMessage());
        }
    }

    private void deleteFile(File file) {
        File[] files;
        if (file == null || !file.exists()) {
            return;
        }
        long fileTime = file.lastModified();
        long currentTime = System.currentTimeMillis();
        if (currentTime - fileTime < (long)this.hour) {
            return;
        }
        for (File f : files = file.listFiles()) {
            if (f.isDirectory()) {
                this.deleteFile(f);
                continue;
            }
            f.delete();
        }
        file.delete();
    }
}

