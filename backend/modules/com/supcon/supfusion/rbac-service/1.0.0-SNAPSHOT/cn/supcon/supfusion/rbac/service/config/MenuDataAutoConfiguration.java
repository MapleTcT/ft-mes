package cn.supcon.supfusion.rbac.service.config;

import com.supcon.supfusion.framework.boot.cloud.events.TenantMessageAutoConfiguration;
import com.supcon.supfusion.framework.boot.scaffold.dbp.MultiTenantDataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author tomcat
 * @date 20-12-25 下午2:58
 */
@Configuration
@AutoConfigureAfter(TenantMessageAutoConfiguration.class)
@AutoConfigureBefore(MultiTenantDataSourceAutoConfiguration.class)
@Import(MenuDataInitializer.class)
public class MenuDataAutoConfiguration {}
