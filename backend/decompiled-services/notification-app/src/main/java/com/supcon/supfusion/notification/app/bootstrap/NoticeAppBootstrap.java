/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.scheduling.annotation.EnableScheduling
 */
package com.supcon.supfusion.notification.app.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.notification.app"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages={"com.supcon.supfusion.ws.client", "com.supcon.supfusion.notification.admin.api"})
public class NoticeAppBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(NoticeAppBootstrap.class, (String[])args);
        System.out.println("----------------\u542f\u52a8\u7ed3\u675f-------------------");
    }
}

