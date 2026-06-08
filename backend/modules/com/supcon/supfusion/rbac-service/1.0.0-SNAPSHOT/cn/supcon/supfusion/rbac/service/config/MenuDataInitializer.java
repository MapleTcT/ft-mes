package cn.supcon.supfusion.rbac.service.config;

import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.cloud.common.constants.SystemConstant;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.scaffold.dbp.util.DatabaseInitializer;
import com.supcon.supfusion.framework.scaffold.dbp.util.DatabaseInitializerEmitter;
import com.supcon.supfusion.rbac.service.config.LaunchInitialize;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tomcat
 * @date 20-9-11 下午10:03
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@Setter
@Getter
public class MenuDataInitializer implements DatabaseInitializer, InitializingBean, CommandLineRunner {

    @Autowired
    @Lazy
    private LaunchInitialize launchInitialize;
    @Autowired
    @Lazy
    private DataSourceConnectionProperties dataSourceConnectionProperties;

    private static final Map<String, Object> INITIALIZED_TENANT_MAP = new ConcurrentHashMap<>();

    @Override
    public void init(TenantInfo tenantInfo) {
        if (!INITIALIZED_TENANT_MAP.containsKey(tenantInfo.getId())) {
            INITIALIZED_TENANT_MAP.put(tenantInfo.getId(), true);
            log.info("==> do menu data initializing after datasource created, tenant={}", tenantInfo.toString());
            RpcContext.getContext().setTenantId(tenantInfo.getId());
            launchInitialize.run(tenantInfo.getId());
        }
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("==> add menu data initializer to emitter");
        DatabaseInitializerEmitter.add(this);
    }

    @Override
    public void run(String... args) throws Exception {
        if (dataSourceConnectionProperties.getUseSystem() && !INITIALIZED_TENANT_MAP.containsKey(SystemConstant.SYSTEM_TENANT_ID)) {
            INITIALIZED_TENANT_MAP.put(SystemConstant.SYSTEM_TENANT_ID, true);
            log.info("==> do menu data initializing after datasource created, tenant={}", SystemConstant.SYSTEM_TENANT_ID);
            RpcContext.getContext().setTenantId(SystemConstant.SYSTEM_TENANT_ID);
            launchInitialize.run(SystemConstant.SYSTEM_TENANT_ID);
        }
    }
}
