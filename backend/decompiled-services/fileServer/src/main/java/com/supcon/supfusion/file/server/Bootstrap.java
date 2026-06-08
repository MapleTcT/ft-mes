/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.scheduling.annotation.EnableScheduling
 *  springfox.documentation.swagger2.annotations.EnableSwagger2
 */
package com.supcon.supfusion.file.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.file.server"})
@MapperScan(value={"com.supcon.supfusion.file.server.dao"})
@EnableScheduling
@EnableDiscoveryClient
@EnableSwagger2
@EnableFeignClients(basePackages={"com.supcon.supfusion.file.server.api", "com.supcon.supfusion.tenant.api", "com.supcon.supfusion.organization.api"})
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

