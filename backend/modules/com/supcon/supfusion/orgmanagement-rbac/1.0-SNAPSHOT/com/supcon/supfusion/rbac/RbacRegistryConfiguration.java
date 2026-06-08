package com.supcon.supfusion.rbac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author: kk.c
 * @Date: 2020/9/29 18:08
 * @since
 */
@MapperScan("com.supcon.supfusion.rbac.dao")
@EnableFeignClients({
        "com.supcon.supfusion.i18n.service.api",
        "com.supcon.supfusion.module.registry.api",
        "com.supcon.supfusion.systemcode.api",
        "com.supcon.supfusion.tenant.api"})
//@EnableBinding({TenantEventSink.class, RbacEventSink.class})
@ComponentScan(value = {"com.supcon.supfusion.rbac"})
public class RbacRegistryConfiguration {

}
