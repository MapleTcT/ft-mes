/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 */
package com.supcon.supfusion.auditlog.bootstrap;

import java.io.IOException;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.auditlog"})
@MapperScan(value={"com.supcon.supfusion.auditlog.dao"})
@EnableDiscoveryClient
@EnableFeignClients(value={"com.supcon.supfusion.systemcode.api", "com.supcon.supfusion.i18n.service.api", "com.supcon.supfusion.file.server.api"})
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

