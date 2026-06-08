package com.supcon.supfusion.counter.sdk.spring;

import com.supcon.supfusion.counter.api.CounterService;
import com.supcon.supfusion.counter.sdk.CounterServiceAdapter;
import com.supcon.supfusion.counter.sdk.generator.DefaultCodeGenerator;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableFeignClients(clients = CounterService.class)
@Import(value = {CounterServiceAdapter.class, DefaultCodeGenerator.class})
public class CounterSDKAutoConfiguration {

}
