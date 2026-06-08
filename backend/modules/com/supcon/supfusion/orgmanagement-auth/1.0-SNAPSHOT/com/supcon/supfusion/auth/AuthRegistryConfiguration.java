package com.supcon.supfusion.auth;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import com.supcon.supfusion.auth.service.kafka.AuthUserStream;

@MapperScan("com.supcon.supfusion.auth.dao")
@EnableSwagger2
@EnableFeignClients({
        "com.supcon.supfusion.rbac.api",
        "com.supcon.supfusion.organization.api",
        "com.supcon.supfusion.systemcode.api",
        "com.supcon.supfusion.auth.manager.feign.client",
        "com.supcon.supfusion.tenant.api",
        "com.supcon.supfusion.ws.client",
        "com.supcon.supfusion.notification.apiserver.api",
        "com.supcon.supfusion.iam.api",
        "com.supcon.supfusion.notification.admin.api",
        "com.supcon.supfusion.flow.api"
})
@EnableBinding({TenantEventSink.class, AuthUserStream.class})
@EnableScheduling
@ComponentScan(value = {"com.supcon.supfusion.auth"})
public class AuthRegistryConfiguration {

}
