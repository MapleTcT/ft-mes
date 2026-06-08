package cn.supcon.supfusion.systemconfig.config;

import cn.supcon.supfusion.systemconfig.config.filters.AnnoFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author tomcat
 * @date 20-10-21 下午1:55
 */
@Slf4j
@Configuration
@ConditionalOnMissingClass({
        "com.supcon.orchid.entityconf.MicroService",
        "com.supcon.supfusion.systemconfig.SystemConfigBootstrap"
})
public class SystemConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AnnoFilter annoFilter() {
        if (log.isDebugEnabled()) {
            log.debug("==> success to register bean: {}", AnnoFilter.class.getName());
        }
        return new AnnoFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RefreshLocalConfigFromRemote refreshLocalConfigFromRemote() {
        return new RefreshLocalConfigFromRemote();
    }
}
