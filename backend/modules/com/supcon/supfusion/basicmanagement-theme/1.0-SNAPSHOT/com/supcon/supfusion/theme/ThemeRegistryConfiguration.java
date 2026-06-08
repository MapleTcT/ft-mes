package com.supcon.supfusion.theme;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = {
        "com.supcon.supfusion.theme",
})
@MapperScan(basePackages = "com.supcon.supfusion.theme.dao")
public class ThemeRegistryConfiguration {
}
