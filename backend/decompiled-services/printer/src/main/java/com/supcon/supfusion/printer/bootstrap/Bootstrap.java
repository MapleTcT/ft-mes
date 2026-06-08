/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  springfox.documentation.swagger2.annotations.EnableSwagger2
 */
package com.supcon.supfusion.printer.bootstrap;

import java.io.IOException;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.printer"})
@MapperScan(value={"com.supcon.supfusion.printer.dao"})
@EnableDiscoveryClient
@EnableFeignClients(value={"com.supcon.supfusion.i18n", "com.supcon.supfusion.file.server.api", "com.supcon.supfusion.tenant.api"})
@EnableSwagger2
public class Bootstrap {
    public static void main(String[] args) throws IOException {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

