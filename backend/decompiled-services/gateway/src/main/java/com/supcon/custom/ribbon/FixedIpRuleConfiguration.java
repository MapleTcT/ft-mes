/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.netflix.loadbalancer.IRule
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.custom.ribbon;

import com.netflix.loadbalancer.IRule;
import com.supcon.custom.ribbon.FixedIpRibbonRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FixedIpRuleConfiguration {
    @Bean
    public IRule fixedIpRule() {
        return new FixedIpRibbonRule();
    }
}

