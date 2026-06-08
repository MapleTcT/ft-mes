package com.supcon.supfusion.systemcode;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = {"com.supcon.supfusion.systemcode"})
@MapperScan("com.supcon.supfusion.systemcode.dao")
@EnableFeignClients({
        "com.supcon.supfusion.module",
        "com.supcon.supfusion.organization",
        "com.supcon.supfusion.i18n.service.api",
        "com.supcon.supfusion.tenant.api"
})
@EnableBinding(TenantEventSink.class)
public class SystemCodeRegistryConfiguration {
}
