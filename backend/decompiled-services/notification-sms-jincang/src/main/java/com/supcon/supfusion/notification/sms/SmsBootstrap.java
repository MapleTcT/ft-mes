/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.scheduling.annotation.EnableScheduling
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.supfusion.notification.sms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

@EnableScheduling
@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.notification.sms"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages={"com.supcon.supfusion.notification.admin"})
@RestController
public class SmsBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(SmsBootstrap.class, (String[])args);
    }
}

