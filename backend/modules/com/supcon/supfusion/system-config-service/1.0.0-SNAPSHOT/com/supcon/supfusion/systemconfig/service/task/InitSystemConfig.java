package com.supcon.supfusion.systemconfig.service.task;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.systemconfig.common.constants.Constants;
import com.supcon.supfusion.systemconfig.common.util.SystemUtil;
import com.supcon.supfusion.systemconfig.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@Slf4j
public class InitSystemConfig implements ApplicationRunner {

    @Autowired
    private SystemConfigService systemConfigService;

//    /**
//     * 初始化配置数据
//     */
//    @Bean
//    public void init() {
//        systemConfigService.initSystemConfig();
//    }

//    /**
//     * 定时任务刷新数据及版本缓存
//     */
////    @Scheduled(cron = "*/30 * * * * ?")
//    @Bean
//    public void scheduleRefreshConfigCache() {
//        systemConfigService.scheduleRefreshConfigCache();
//    }

    @Override
    public void run(ApplicationArguments args) {
        Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
        log.info("最初租户信息,tenantInfoSet:{}", JSON.toJSONString(tenantInfoSet));
        systemConfigService.scheduleRefreshConfigCache();
    }

    @Bean
    public void initConfig() {
        if (Constants.WINDOWS.equalsIgnoreCase(SystemUtil.getOS())) {
            systemConfigService.deleteBatchIds("baseImages");
        }
    }
}
