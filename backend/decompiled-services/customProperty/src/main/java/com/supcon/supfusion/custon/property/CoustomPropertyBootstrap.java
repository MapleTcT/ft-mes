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
package com.supcon.supfusion.custon.property;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages={"com.supcon.supfusion.custon.property"})
@MapperScan(basePackages={"com.supcon.supfusion.custon.property.dao.mappers"})
@EnableAspectJAutoProxy
@EnableDiscoveryClient
@EnableFeignClients(basePackages={"com.supcon.supfusion.i18n.service.api", "com.supcon.supfusion.tenant.api", "com.supcon.supfusion.systemcode.api"})
@EnableBinding(value={TenantEventSink.class})
public class CoustomPropertyBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(CoustomPropertyBootstrap.class, (String[])args);
    }
}

