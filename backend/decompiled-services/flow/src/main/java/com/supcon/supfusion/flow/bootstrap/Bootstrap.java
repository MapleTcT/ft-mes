/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.context.annotation.EnableAspectJAutoProxy
 *  springfox.documentation.swagger2.annotations.EnableSwagger2
 */
package com.supcon.supfusion.flow.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.flow"})
@MapperScan(value={"com.supcon.supfusion.flow.dao"})
@EnableSwagger2
@EnableDiscoveryClient
@EnableFeignClients(basePackages={"com.supcon.supfusion.auth.api", "com.supcon.supfusion.organization.api", "com.supcon.supfusion.tenant.api", "com.supcon.supfusion.ws.client", "com.supcon.supfusion.notification.apiserver.api", "com.supcon.supfusion.notification.admin.api", "com.supcon.supfusion.flow.api"})
@EnableAspectJAutoProxy(exposeProxy=true)
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

