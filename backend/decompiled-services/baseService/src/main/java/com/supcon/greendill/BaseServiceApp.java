/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.supfusion.framework.scaffold.auditlog.config.AuditLogConfiguration
 *  com.supcon.supfusion.framework.scaffold.kafka.tenant.EnableTenantEventBinding
 *  com.supcon.supfusion.framework.scaffold.mongodb.SupMongodbAutoConfiguration
 *  org.springframework.boot.SpringApplication
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
 *  org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
 *  org.springframework.cloud.client.discovery.EnableDiscoveryClient
 *  org.springframework.cloud.openfeign.EnableFeignClients
 *  org.springframework.context.annotation.ComponentScan
 */
package com.supcon.greendill;

import com.supcon.supfusion.framework.scaffold.auditlog.config.AuditLogConfiguration;
import com.supcon.supfusion.framework.scaffold.kafka.tenant.EnableTenantEventBinding;
import com.supcon.supfusion.framework.scaffold.mongodb.SupMongodbAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableDiscoveryClient
@EnableFeignClients(value={"com.supcon.supfusion"})
@SpringBootApplication(exclude={HibernateJpaAutoConfiguration.class, MongoAutoConfiguration.class, SupMongodbAutoConfiguration.class, AuditLogConfiguration.class})
@ComponentScan(value={"com.supcon.orchid", "com.supcon.greendill"})
@EnableTenantEventBinding
public class BaseServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(BaseServiceApp.class, (String[])args);
        System.out.println("====\u542f\u52a8\u6210\u529f====");
    }
}

