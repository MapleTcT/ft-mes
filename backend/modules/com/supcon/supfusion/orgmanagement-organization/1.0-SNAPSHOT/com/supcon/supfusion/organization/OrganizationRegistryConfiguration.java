package com.supcon.supfusion.organization;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;
import com.supcon.supfusion.organization.common.kafka.OrganizationStream;

/**
 * @Author: fukun
 * @Date: 2020/9/22 8:20
 * @since
 */
@MapperScan("com.supcon.supfusion.organization.dao")
@EnableFeignClients({
        "com.supcon.supfusion.auth.api",
        "com.supcon.supfusion.rbac.api",
        "com.supcon.supfusion.systemcode.api",
        "com.supcon.supfusion.tenant.api",
        "com.supcon.supfusion.file.server.api"
})
@ComponentScan(value = {"com.supcon.supfusion.organization"})
@EnableBinding({TenantEventSink.class, OrganizationStream.class})
public class OrganizationRegistryConfiguration {

}
