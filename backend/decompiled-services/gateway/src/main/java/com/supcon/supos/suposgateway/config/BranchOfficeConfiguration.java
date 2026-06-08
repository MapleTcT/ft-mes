/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.AutoConfigureAfter
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.cloud.gateway.config.GatewayAutoConfiguration
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supos.suposgateway.config;

import com.supcon.supos.suposgateway.filter.BranchOfficeGlobalFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name={"supfusion.auth.branch-office.enable"}, havingValue="true")
@AutoConfigureAfter(value={GatewayAutoConfiguration.class})
public class BranchOfficeConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchOfficeConfiguration.class);

    @Bean
    public BranchOfficeGlobalFilter headBranchOfficeGlobalFilter() {
        LOGGER.info("Branch office enabled");
        return new BranchOfficeGlobalFilter();
    }
}

