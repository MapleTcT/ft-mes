package com.supcon.supfusion.printer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author liyiming
 * @date 2020/11/30 10:30 上午
 */
@Component
@Slf4j
public class NacosInstance {

    public String selectOneHealthyInstance(Integer source, String url) {
        return url;
    }
}
