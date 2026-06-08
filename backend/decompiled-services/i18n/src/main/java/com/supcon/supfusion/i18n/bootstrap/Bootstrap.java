/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.context.annotation.ComponentScan
 *  org.springframework.scheduling.annotation.EnableScheduling
 *  springfox.documentation.swagger2.annotations.EnableSwagger2
 */
package com.supcon.supfusion.i18n.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@MapperScan(value={"com.supcon.supfusion.i18n.dao"})
@ComponentScan(basePackages={"com.supcon.supfusion.i18n"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages={"com.supcon.supfusion.module.registry.api", "com.supcon.supfusion.tenant.api"})
@EnableScheduling
@EnableSwagger2
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

