package com.supcon.supfusion.systemconfig.api.tenantconfig;

import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.tenantconfig.filter.AnnoFilter;
import com.supcon.supfusion.systemconfig.api.tenantconfig.task.RefreshConfigInfoTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 注册国际化资源
 *
 * @author ricky
 * @version 1.0.0
 * @date 2020-06-30 17:56
 * @copyright
 */
@Slf4j
@Configuration
@ConditionalOnMissingClass({"com.supcon.orchid.entityconf.MicroService",
        "com.supcon.supfusion.systemconfig.SystemConfigBootstrap"})
public class SystemConfig {

    @Bean("annoFilter")
    public AnnoFilter annoFilter() {
        log.info("将注解过滤器加载到spring容器中 =======");
        return new AnnoFilter();
    }

    @Autowired
    private SystemApiService systemApiService;


    @Bean
    public RefreshConfigInfoTask refreshConfigInfoTask() {
        log.info("将任务执行类加载到spring容器中 ==========");
        RefreshConfigInfoTask refreshConfigInfoTask = new RefreshConfigInfoTask();
        refreshConfigInfoTask.refreshConfigInfo(systemApiService);
        return refreshConfigInfoTask;
    }

}
