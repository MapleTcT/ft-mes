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
package com.supcon.supfusion.notification.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.notification.mobile"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages={"com.supcon.supfusion.ws.client", "com.supcon.supfusion.notification.admin.api"})
public class MobileBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(MobileBootstrap.class, (String[])args);
    }
}

