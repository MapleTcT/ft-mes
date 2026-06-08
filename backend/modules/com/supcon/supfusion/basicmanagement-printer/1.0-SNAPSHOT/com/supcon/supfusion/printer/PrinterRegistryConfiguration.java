package com.supcon.supfusion.printer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = {"com.supcon.supfusion.printer"})
@MapperScan("com.supcon.supfusion.printer.dao")
@EnableFeignClients({"com.supcon.supfusion.i18n", "com.supcon.supfusion.file.server.api"})
public class PrinterRegistryConfiguration {
}
