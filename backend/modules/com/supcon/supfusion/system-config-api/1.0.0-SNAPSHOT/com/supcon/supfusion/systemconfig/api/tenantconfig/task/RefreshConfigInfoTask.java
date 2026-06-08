package com.supcon.supfusion.systemconfig.api.tenantconfig.task;

import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.dto.ConfigAndVersionDTO;
import com.supcon.supfusion.systemconfig.api.tenantconfig.util.DateHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//@Component
@Slf4j
public class RefreshConfigInfoTask {

    public static ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>>
            configMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> versionMap = new ConcurrentHashMap<>();


    private static final ScheduledExecutorService
            SCHEDULED_CONFIG = Executors.newSingleThreadScheduledExecutor();


    public void refreshConfigInfo(SystemApiService systemApiService) {
        SCHEDULED_CONFIG.scheduleAtFixedRate(() -> this.executeConfigInfo(systemApiService), 10, 20 * DateHelper.SECOND_TIME, TimeUnit.MILLISECONDS);
    }

    public void executeConfigInfo(SystemApiService systemApiService) {
        try {
            log.debug("框架中定时任务根据版本获取配置数据，配置版本 开始执行 =============");
            //根据版本号得到和配置系统中有差异的版本，获取这部分有差异版本对应的配置数据
            ConfigAndVersionDTO configAndVersionDTO = systemApiService.getConfigByVersionForFramework(versionMap);
            if (configAndVersionDTO.getIsUpdate()) {
                configMap = configAndVersionDTO.getConfigMap();
                versionMap = configAndVersionDTO.getVersionMap();
            }
        } catch (Exception e) {
            log.error("框架中定时任务根据版本获取配置数据，配置版本 发生错误 =============", e);
        }
    }


}
