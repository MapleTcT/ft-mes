package com.supcon.supfusion.protal;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = {"com.supcon.supfusion.portal", "com.supcon.supfusion.portal.api", "com.supcon.supfusion.protal.service"})
@MapperScan(basePackages = "com.supcon.supfusion.portal.dao")
@EnableFeignClients({"com.supcon.supfusion.i18n.service.api", "com.supcon.supfusion.rbac.api"})
public class PortalRegistryConfiguration {
}
