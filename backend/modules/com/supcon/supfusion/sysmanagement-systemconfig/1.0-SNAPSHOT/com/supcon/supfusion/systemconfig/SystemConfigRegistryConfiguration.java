package com.supcon.supfusion.systemconfig;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ComponentScan(value = {
        "com.supcon.supfusion.systemconfig",
})
@EnableTransactionManagement
@MapperScan(basePackages = "com.supcon.supfusion.systemconfig.dao")
@EnableFeignClients(basePackages = {
        "com.supcon.supfusion.tenant.api"
})
@EnableBinding(TenantEventSink.class)
@EnableScheduling
public class SystemConfigRegistryConfiguration {
}
