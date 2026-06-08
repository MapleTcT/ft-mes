package com.supcon.supfusion.framework.scaffold.redis.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author chenlizhong
 * @date 2020/5/18下午6:36
 * @description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = SupfusionRedisProperties.PREFIX)
public class SupfusionRedisProperties {

    public static final String PREFIX = "spring.cache";


    private Boolean useTenantPrefix;

    /**
     * Duration
     * Duration 字符串类似数字有正负之分,默认正,负以’-‘开头,紧接着’P’,下面所有字母都不区分大小写:
     * ‘D’ – 天
     * ‘H’ – 小时
     * ‘M’ – 分钟
     * ‘S’ – 秒
     * 字符’T’是紧跟在时分秒之前的，每个单位都必须由数字开始,且时分秒顺序不能乱,比如:P2DT3M5S,P3D,PT3S
     * PT3M2S 等于 -PT-3M-2S
     * <p>
     * 默认为30min
     */
    private String cacheTtl;

    private Boolean cacheNullValues;

}
