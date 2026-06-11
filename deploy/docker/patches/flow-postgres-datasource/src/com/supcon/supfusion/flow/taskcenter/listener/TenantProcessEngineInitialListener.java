package com.supcon.supfusion.flow.taskcenter.listener;

import com.baomidou.mybatisplus.annotation.DbType;
import com.supcon.supfusion.flow.engine.server.config.MultiTenantAwareDataSource;
import com.supcon.supfusion.flow.engine.server.migration.V30EngineDataMigration;
import com.supcon.supfusion.flow.engine.server.register.TenantRegister;
import com.supcon.supfusion.flow.taskcenter.component.DataSourceBuilder;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.cloud.common.events.TenantDatabaseInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.scaffold.dbp.factory.populator.MultiTenantDatabasePopulator;
import com.supcon.supfusion.framework.scaffold.dbp.factory.populator.MultiTenantDatabasePopulatorUtils;
import com.supcon.supfusion.tenant.api.TenantManagerService;
import com.supcon.supfusion.tenant.api.dto.SystemDatabaseDTO;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.impl.cfg.multitenant.MultiSchemaMultiTenantProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 系统启动初始化所有租户数据
 */
@Slf4j
@Component
public class TenantProcessEngineInitialListener implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantProcessEngineInitialListener.class);

    @Autowired
    @Lazy
    private TenantRegister tenantRegister;
    @Autowired
    @Lazy
    private TenantManagerService tenantManagerService;
    @Autowired
    private DataSourceBuilder dataSourceBuilder;
    @Autowired
    private MultiTenantAwareDataSource tenantAwareDataSource;
    @Autowired
    @Lazy
    private MultiTenantDatabasePopulator databasePopulator;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private MultiSchemaMultiTenantProcessEngineConfiguration multiSchemaMultiTenantProcessEngine;
    @Autowired
    private DataSourceConnectionProperties dscp;

    /**
     * <li>1.初始化所有租户的DataSource</li>
     * <li>2.初始化所有租户的数据库</li>
     * <li>3.如果是第一次初始化租户表,需要将系统库数据迁移到租户库,因为3.0版本的数据存放在系统库</li>
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (dscp.getUseSystem().booleanValue()) {
            registerSystemDatabase();
        } else {
            registerSuplakeDatabase();
        }
    }

    private void registerSystemDatabase() {
        final String dtTenant = "dt";
        // 判断是否其他地方在初始化
        boolean acquire = redisUtils.acquire(dtTenant);
        if (!acquire) {
            log.info("租户({})正在初始化, 当前不重复执行", dtTenant);
            return;
        }
        try {
            DataSource systemDataSource = dataSourceBuilder.build(
                      dscp.getSystem().getDbName()
                    , dscp.getSystem().getDbType()
                    , dscp.getSystem().getHost()
                    , dscp.getSystem().getPort()
                    , dscp.getSystem().getUsername()
                    , dscp.getSystem().getPassword()
                    , dtTenant);
            // 开始初始化数据库
            TenantInfo tenantInfo = new TenantInfo();
            TenantDatabaseInfo tdi = new TenantDatabaseInfo();
            tdi.setDbType(dscp.getSystem().getDbType());
            tdi.setDbName(dscp.getSystem().getDbName());
            tenantInfo.setDatabaseInfo(tdi);
            multiSchemaMultiTenantProcessEngine.registerTenant(dtTenant, systemDataSource);
            MultiTenantDatabasePopulatorUtils.execute(databasePopulator, systemDataSource, tenantInfo);
        } finally {
            if (acquire) {
                log.info("租户({})初始化结束, 释放锁, key={}", dtTenant, "flow_" + dtTenant);
                redisUtils.release(dtTenant);
            }
        }

    }

    private void registerSuplakeDatabase() {
        ListResult<TenantDTO> tenants = null;
        try {
            tenants = tenantManagerService.find(null);
            if (tenants.getList() == null || tenants.getList().isEmpty()) {
                log.info("查询租户列表为空, 无需初始化租户数据");
                return;
            }
        } catch (Exception ignore) {
            log.error("无法获取所有租户数据, 系统退出");
            System.exit(0);
        }
        DataSource systemDataSource = null;
        for (TenantDTO tenant : tenants.getList()) {
            boolean registered = true;
            DataSource tenantDataSource = null;
            // 判断是否其他地方在初始化
            boolean acquire = redisUtils.acquire(tenant.getId());
            if (!acquire) {
                log.info("租户({})正在初始化, 当前不重复执行", tenant.getId());
                continue;
            }
            try {
                for (TenantDTO.DatabaseDTO databaseInfo : tenant.getDatabaseInfos()) {
                    // 取主库, 初始化所有租户的数据库
                    if (databaseInfo.getMajor()) {
                        TenantInfo tenantInfo = TenantInfoLocalStorage.get(tenant.getId(), true);
                        tenantDataSource = dataSourceBuilder.build(databaseInfo.getDbName(), databaseInfo.getDbType(), databaseInfo.getHost()
                                , databaseInfo.getPort(), databaseInfo.getUsername(), databaseInfo.getPassword(), tenant.getId());
                        registered = isRegistered(tenantDataSource, databaseInfo.getDbType(), databaseInfo.getDbName(), tenant.getId());
                        // 开始初始化数据库
                        multiSchemaMultiTenantProcessEngine.registerTenant(tenant.getId(), tenantDataSource);
                        MultiTenantDatabasePopulatorUtils.execute(databasePopulator, tenantDataSource, tenantInfo);
                        break;
                    }
                }
                // 如果是第一次初始化租户表,需要将系统库数据迁移到租户库
                if (!registered && tenantDataSource != null) {
                    log.info("开始迁移系统库数据到租户库({})", tenant.getId());
                    if (systemDataSource == null) {
                        SystemDatabaseDTO sysDatabase = tenantManagerService.findSystemDatabaseInfo().getData();
                        systemDataSource = dataSourceBuilder.build(sysDatabase.getDbName(),sysDatabase.getDbType(), sysDatabase.getHost(), sysDatabase.getPort()
                                , sysDatabase.getUsername(), sysDatabase.getPassword(), "system001");
                    }
                    migrateFromSystemToTenant(tenant.getId(), systemDataSource, tenantDataSource);
                    log.info("======= 数据迁移成功 =======");
                }
            } catch (SQLException e) {
                log.error("租户数据初始化失败,租户ID={}", tenant.getId(), e);
            } finally {
                if (acquire) {
                    log.info("租户({})初始化结束, 释放锁, key={}", tenant.getId(), "flow_" + tenant.getId());
                    redisUtils.release(tenant.getId());
                }
            }
        }
    }


    // 将系统库数据迁移到对应的租户库
    private void migrateFromSystemToTenant(String tenantId, DataSource systemDataSource, DataSource tenantDataSource) throws SQLException {
        try (Connection sysConn = systemDataSource.getConnection();
             Connection tenantConn = tenantDataSource.getConnection();) {
            V30EngineDataMigration.migrate(sysConn, tenantConn, tenantId);
        }
    }

    // 判断数租户据库是否已经初始化
    private boolean isRegistered(DataSource dataSource, String dbType, String dbName, String tenantId) throws SQLException {
        Statement stat = null;
        ResultSet resultSet = null;
        try (Connection conn = dataSource.getConnection()) {
            switch(DbType.getDbType(dbType)) {
                case MARIADB: case MYSQL: { // 引擎不支持mariadb
                    resultSet = conn.getMetaData().getTables(dbName, null, "ACT_RU_EXECUTION", new String[]{"TABLE"});
                    return resultSet.next();
                }
                case ORACLE: {
                    stat = conn.createStatement();
                    resultSet = stat.executeQuery("SELECT COUNT(*) FROM user_tables WHERE table_name = 'ACT_RU_EXECUTION'");
                    return resultSet.next() && resultSet.getInt(1) > 0;
                }
                case SQL_SERVER: {
                    stat = conn.createStatement();
                    resultSet = stat.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ACT_RU_EXECUTION'");
                    return resultSet.next() && resultSet.getInt(1) > 0;
                }
                case POSTGRE_SQL: {
                    resultSet = conn.getMetaData().getTables(null, null, "act_ru_execution", new String[]{"TABLE"});
                    return resultSet.next();
                }
            }

        } finally {
            if (stat != null) {
                stat.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
