package com.supcon.supfusion.flow.taskcenter.listener;

import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.scaffold.dbp.DataSourceConfig;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.exception.DBProxyErrorEnum;
import com.supcon.supfusion.framework.scaffold.dbp.exception.DBProxyException;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionMaterial;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MariadbDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MysqlDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.PostgresqlDataSourceProductionLine;
import com.supcon.supfusion.tenant.api.TenantManagerService;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 3.5版本已经做了多租户数据库隔离,不需要再单独做各租户数据库初始化动作
 */
@Deprecated
@Slf4j
//@Component
//@Async
//@ConditionalOnProperty(name = "integration.supos.enabled", matchIfMissing = true)
public class AllTenantInitialCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AllTenantInitialCommandLineRunner.class);

    @Autowired
    private DataSourceConfig dsConfig;
    @Autowired
    @Lazy
    private TenantManagerService tenantManagerService;
    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${supfusion.cloud.datasource.connect.system.db-type}")
    private String dbType;
    private static final String SQL_FILE = "META-INF/custom/flow_tenant_init.sql";
    public static int RETRY_COUNT = 10;

    @Override
    public void run(String... args) throws Exception {
        if ("mariadb".equals(dbType) || "mysql".equals(dbType)) {
            // 临时解决方案,等3.5加入服务依赖
            Thread.sleep(20000);
            final ListResult<TenantDTO> tenants = findAllTenant();
            for (TenantDTO tenant : tenants.getList()) {
                for (TenantDTO.DatabaseDTO databaseInfo : tenant.getDatabaseInfos()) {
                    log.info("开始初始化租户{}更新sql", tenant.getId());
                    // 取主库
                    if (databaseInfo.getMajor()) {
                        DataSource dataSource = buildDataSource(databaseInfo, tenant.getId());
                        executeScript(dataSource, SQL_FILE, databaseInfo.getDbName());
                    }
                }
            }
        }
    }
    
    private ListResult<TenantDTO> findAllTenant() throws InterruptedException {
        ListResult<TenantDTO> tenants = tenantManagerService.find(null);
        // 尝试10次, 避免租户服务没起好
        while (--RETRY_COUNT > 0) {
            if (tenants.getList() == null || tenants.getList().isEmpty()) {
                Thread.sleep(10000);
                tenants = tenantManagerService.find(null);
            } else {
                break;
            }
        }
        return tenants;
    }

    private DataSource buildDataSource(TenantDTO.DatabaseDTO  databaseInfo, String tenantId) {
        DataSourceProductionMaterial material = DataSourceProductionMaterial.builder()
                .dbName(databaseInfo.getDbName())
                .host(databaseInfo.getHost())
                .port(databaseInfo.getPort())
                .username(databaseInfo.getUsername())
                .password(databaseInfo.getPassword()).build();
        String var5 = DbType.getDbType(databaseInfo.getDbType());
        Object line;
        switch(var5) {
            case "mariadb":
                line = new MariadbDataSourceProductionLine();
                break;
            case "mysql":
                line = new MysqlDataSourceProductionLine();
                break;
            case "postgresql":
            case "postgres":
                line = new PostgresqlDataSourceProductionLine();
                break;
            default:
                throw new DBProxyException(DBProxyErrorEnum.NOT_SUPPORT_TYPE);
        }
        return ((DataSourceProductionLine)line).build(material, this.dsConfig, tenantId);
    }

    private void executeScript(DataSource dataSource, String scriptFile, String dbName) {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resource = null;
        Statement stat = null;
        InputStreamReader inStreamReaderUpdate = null;
        try (Connection conn = dataSource.getConnection()) {
            resource = resolver.getResources(scriptFile);
            conn.setCatalog(dbName);
            stat = conn.createStatement();
            inStreamReaderUpdate = new InputStreamReader(resource[0].getInputStream(), StandardCharsets.UTF_8);
            String sql = new String(IOUtils.toString(inStreamReaderUpdate).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).replaceAll("[\n]", " ");
            String[] sqlArr = sql.split(";");
            for (String sqlStr : sqlArr) {
                if (StringUtils.isNotEmpty(sqlStr.trim())) {
                    try {
                        stat.execute(sqlStr);
                    } catch (Exception ignore) {
                        // ignore exception
                    }
                }
            }
        } catch (Exception ignore) {
            // ignore exception
        } finally {
            if (inStreamReaderUpdate != null) {
                try {
                    inStreamReaderUpdate.close();
                } catch (Exception e) {
                    log.error("关闭InputStreamReader失败", e);
                }
            }
            if (stat != null) {
                try {
                    stat.close();
                } catch (SQLException close) {
                    //
                }
            }
        }
    }
}
