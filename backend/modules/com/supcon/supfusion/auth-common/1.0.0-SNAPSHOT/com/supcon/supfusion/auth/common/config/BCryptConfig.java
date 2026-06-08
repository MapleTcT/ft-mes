package com.supcon.supfusion.auth.common.config;

import com.supcon.supfusion.auth.common.utils.BCryptUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Data
public class BCryptConfig {
    @Value("${nationalEncry-enable:false}")
    private boolean enableNationalEncry;

    @PostConstruct
    public void init(){
        BCryptUtil.setConfigInfo(this);
    }
}
