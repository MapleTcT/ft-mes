package com.supcon.supfusion.framework.scaffold.auditlog.config;

import com.supcon.supfusion.framework.scaffold.auditlog.aspect.AuditBusinessLogAspect;
import com.supcon.supfusion.framework.scaffold.auditlog.aspect.AuditLogAspect;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.IModelCacheService;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.ModelAuditLogCache;
import com.supcon.supfusion.framework.scaffold.auditlog.cache.ModelCacheServiceImpl;
import com.supcon.supfusion.framework.scaffold.auditlog.filter.AuditDataLogFilter;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditBusinessLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.pojo.bo.AuditDataLogBO;
import com.supcon.supfusion.framework.scaffold.auditlog.propreties.AuditLogProperties;
import com.supcon.supfusion.framework.scaffold.auditlog.strategy.AuditLogStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 审计日志配置
 *
 * @author caokele
 */
@EnableAsync
@Configuration
@ComponentScan({
        "com.supcon.supfusion.framework.scaffold.auditlog.repository",
        "com.supcon.supfusion.framework.scaffold.auditlog.event",
        "com.supcon.supfusion.framework.scaffold.auditlog.strategy"
})
@EnableConfigurationProperties({AuditLogProperties.class})
public class AuditLogConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AuditBusinessLogAspect auditBusinessLogAspect(AuditLogStrategy<AuditBusinessLogBO> auditLogStrategy) {
        AuditBusinessLogAspect auditBusinessLogAspect = new AuditBusinessLogAspect();
        auditBusinessLogAspect.setAuditLogStrategy(auditLogStrategy);
        return auditBusinessLogAspect;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogAspect auditLogAspect(AuditLogStrategy<AuditBusinessLogBO> auditBusinessLogStrategy, AuditLogStrategy<AuditDataLogBO> auditDataLogStrategy) {
        AuditLogAspect auditLogAspect = new AuditLogAspect();
        auditLogAspect.setAuditBusinessLogStrategy(auditBusinessLogStrategy);
        auditLogAspect.setAuditDataLogStrategy(auditDataLogStrategy);
        return auditLogAspect;
    }

    @Bean
    @ConditionalOnMissingBean
    public IModelCacheService modelCacheService() {
        return new ModelCacheServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelAuditLogCache modelAuditLogCache(IModelCacheService modelCacheService) {
        ModelAuditLogCache modelAuditLogCache = new ModelAuditLogCache();
        modelAuditLogCache.setModelCache(modelCacheService);
        return modelAuditLogCache;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditDataLogFilter auditDataLogFilter() {
        return new AuditDataLogFilter();
    }
}
