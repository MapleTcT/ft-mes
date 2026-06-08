package com.supcon.supfusion.signature;

import com.supcon.supfusion.signature.services.kafka.SignatureEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;

@ComponentScan(value = {"com.supcon.supfusion.signature"})
@MapperScan(basePackages = {"com.supcon.supfusion.signature.dao.mappers"})
@EnableTransactionManagement
@EnableAsync
@EnableFeignClients(basePackages = {
        "com.supcon.supfusion.rbac.api",
        "com.supcon.supfusion.organization.api",
        "com.supcon.supfusion.i18n.service.api",
        "com.supcon.supfusion.module.registry.api",
        "com.supcon.supfusion.tenant.api",
        "com.supcon.supfusion.auth.api"
})
@EnableScheduling
@EnableBinding({SignatureEventSink.class, TenantEventSink.class})
public class SignatureRegistryConfiguration {
}
