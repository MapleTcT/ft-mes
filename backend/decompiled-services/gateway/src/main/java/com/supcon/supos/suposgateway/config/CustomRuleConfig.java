/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.cloud.netflix.ribbon.RibbonClients
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supos.suposgateway.config;

import com.supcon.custom.ribbon.FixedIpRuleConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@RibbonClients(defaultConfiguration={FixedIpRuleConfiguration.class})
@ConditionalOnProperty(name={"custom.dev.enable"}, havingValue="true")
public class CustomRuleConfig {
}

