package com.supcon.supfusion.module.registry;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = "com.supcon.supfusion.module.registry")
@MapperScan("com.supcon.supfusion.module.registry.dao.mapper")
@EnableFeignClients(value = {
        "com.supcon.supfusion.tenant.api",
        "com.supcon.supfusion.i18n.service.api"
})
@EnableBinding(TenantEventSink.class)
public class ModuleRegistryRegistryConfiguration {
}
