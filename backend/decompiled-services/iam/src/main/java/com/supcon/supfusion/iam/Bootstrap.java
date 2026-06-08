/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink
 *  org.mybatis.spring.annotation.MapperScan
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.cloud.stream.annotation.EnableBinding
 *  org.springframework.context.annotation.EnableAspectJAutoProxy
 *  org.springframework.transaction.annotation.EnableTransactionManagement
 */
package com.supcon.supfusion.iam;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.iam"})
@MapperScan(basePackages={"com.supcon.supfusion.iam.dao"})
@EnableFeignClients(basePackages={"com.supcon.supfusion.tenant.api", "com.supcon.supfusion.auth.keycloak.client.api"})
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableDiscoveryClient
@EnableBinding(value={TenantEventSink.class})
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

