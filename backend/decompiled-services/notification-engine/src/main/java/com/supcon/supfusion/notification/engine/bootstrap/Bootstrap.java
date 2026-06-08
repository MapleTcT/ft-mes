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
 */
package com.supcon.supfusion.notification.engine.bootstrap;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.notification"})
@MapperScan(value={"com.supcon.supfusion.notification.engine.dao.mappers", "com.supcon.supfusion.notification.email.dao.mappers", "com.supcon.supfusion.notification.stationLetter.dao.mappers"})
@EnableAspectJAutoProxy
@EnableDiscoveryClient
@EnableFeignClients(value={"com.supcon.supfusion.tenant.api", "com.supcon.supfusion.ws.client"})
@EnableBinding(value={TenantEventSink.class})
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, (String[])args);
    }
}

