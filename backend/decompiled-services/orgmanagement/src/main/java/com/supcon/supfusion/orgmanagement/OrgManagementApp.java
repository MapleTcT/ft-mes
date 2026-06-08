/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.context.annotation.ComponentScan
 */
package com.supcon.supfusion.orgmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value={"com.supcon.supfusion.orgmanagement", "com.supcon.supfusion.auth", "com.supcon.supfusion.organization", "com.supcon.supfusion.rbac"})
@SpringBootApplication
@EnableDiscoveryClient
public class OrgManagementApp {
    public static void main(String[] args) {
        SpringApplication.run(OrgManagementApp.class, (String[])args);
    }
}

