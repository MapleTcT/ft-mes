package com.supcon.supfusion.counter;

import com.supcon.supfusion.framework.scaffold.kafka.tenant.TenantEventSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ComponentScan(value = {"com.supcon.supfusion.counter"})
@MapperScan(basePackages = {"com.supcon.supfusion.counter.dao"})
@EnableAspectJAutoProxy
@EnableTransactionManagement
@EnableFeignClients(basePackages = {"com.supcon.supfusion.tenant.api"})
@EnableBinding(TenantEventSink.class)
public class CounterRegistryConfiguration {
}
