/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.scheduling.annotation.EnableAsync
 *  org.springframework.scheduling.annotation.EnableScheduling
 */
package com.supcon.supos.suposgateway.filter.analysis;

import com.supcon.supos.suposgateway.filter.analysis.SaveFileScheduler;
import com.supcon.supos.suposgateway.filter.analysis.SaveFileWorker;
import com.supcon.supos.suposgateway.filter.analysis.UrlAnalysisGlobalFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProperty(name={"url-analysis.root-path"})
@Configuration
@EnableAsync
@EnableScheduling
public class UrlAnalysisConfiguration {
    @Bean
    public UrlAnalysisGlobalFilter urlAnalysisGlobalFilter() {
        return new UrlAnalysisGlobalFilter();
    }

    @Bean
    public SaveFileScheduler saveFileScheduler() {
        return new SaveFileScheduler();
    }

    @Bean
    public SaveFileWorker saveFileWorker() {
        return new SaveFileWorker();
    }
}

