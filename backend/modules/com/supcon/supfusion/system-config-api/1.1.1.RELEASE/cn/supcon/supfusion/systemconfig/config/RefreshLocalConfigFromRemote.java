package cn.supcon.supfusion.systemconfig.config;

import com.supcon.supfusion.framework.cloud.common.thread.NamedThreadFactory;
import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.dto.ConfigAndVersionDTO;
import cn.supcon.supfusion.systemconfig.config.utils.DateHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;

import java.util.HashMap;
import java.util.concurrent.*;

/**
 * @author tomcat
 * @date 20-10-21 下午4:15
 */
@Setter
@Getter
@Slf4j
public class RefreshLocalConfigFromRemote implements ApplicationListener<ApplicationStartedEvent> {

    @Autowired
    @Lazy
    private SystemApiService systemApiService;

    public static ConcurrentHashMap<String, ConcurrentHashMap<String, HashMap<String, Object>>> configMap  = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String>                                             versionMap = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService SCHEDULED_CONFIG = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Refresh-Config"));

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("start a task to synchronize with remote system config server, delay=10, period=20 seconds");
        }
        SCHEDULED_CONFIG.scheduleAtFixedRate(() -> this.executeConfigInfo(systemApiService), 10, 20 * DateHelper.SECOND_TIME, TimeUnit.MILLISECONDS);
    }

    private void executeConfigInfo(SystemApiService systemApiService) {
        try {
            //根据版本号得到和配置系统中有差异的版本，获取这部分有差异版本对应的配置数据
            ConfigAndVersionDTO configAndVersionDTO = systemApiService.getConfigByVersionForFramework(versionMap);
            if (configAndVersionDTO.getIsUpdate()) {
                configMap = configAndVersionDTO.getConfigMap();
                versionMap = configAndVersionDTO.getVersionMap();
            }
        } catch (Exception e) {
            log.error("fail to synchronize with remote system config", e);
        }
    }
}
