package com.supcon.supfusion.flow.engine.server.register;

import com.baomidou.mybatisplus.annotation.DbType;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.DBProxyException;
import com.supcon.supfusion.flow.engine.server.config.MultiTenantAwareDataSource;
import com.supcon.supfusion.framework.boot.scaffold.dbp.MultiTenantDataSourceProperties;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionMaterial;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MssqlDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MysqlDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.OracleDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.PostgresqlDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.populator.MultiTenantDatabasePopulator;
import com.supcon.supfusion.framework.scaffold.dbp.factory.populator.MultiTenantDatabasePopulatorUtils;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import org.flowable.engine.impl.cfg.multitenant.MultiSchemaMultiTenantProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class TenantRegister {

    @Autowired
    @Lazy
    private MultiTenantDataSourceProperties dsConfig;
    @Autowired
    private MultiTenantAwareDataSource tenantAwareDataSource;
    @Autowired
    @Lazy
    private MultiTenantDatabasePopulator databasePopulator;
    @Autowired
    private MultiSchemaMultiTenantProcessEngineConfiguration multiSchemaMultiTenantProcessEngine;

    /**
     * 注册单个租户
     * @param tenant
     */
    public boolean register(TenantDTO tenant) throws SQLException {
        for (TenantDTO.DatabaseDTO databaseInfo : tenant.getDatabaseInfos()) {
            // 取主库
            if (databaseInfo.getMajor()) {
                TenantInfo tenantInfo = TenantInfoLocalStorage.get(tenant.getId(), true);
                DataSource dataSource = buildDataSource(databaseInfo, tenant.getId());
                boolean registered = isRegistered(dataSource, databaseInfo.getDbType(), tenant.getId());
                multiSchemaMultiTenantProcessEngine.registerTenant(tenant.getId(), dataSource);
                MultiTenantDatabasePopulatorUtils.execute(databasePopulator, dataSource, tenantInfo);
                return registered;
            }
        }
        return true;
    }

    private boolean isRegistered(DataSource dataSource, String dbType, String tenantId) throws SQLException {
        Statement stat = null;
        ResultSet resultSet = null;
        try (Connection conn = dataSource.getConnection()) {
            switch(DbType.getDbType(dbType)) {
                case MARIADB: case MYSQL: { // 引擎不支持mariadb
                    resultSet = conn.getMetaData().getTables(tenantId, null, "ACT_RU_EXECUTION", new String[]{"TABLE"});
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

    /**
     * 构建数据源
     * @param databaseInfo
     * @param tenantId
     * @return
     */
    public DataSource buildDataSource(TenantDTO.DatabaseDTO  databaseInfo, String tenantId) {
        DataSourceProductionMaterial material = DataSourceProductionMaterial.builder()
                .dbName(databaseInfo.getDbName())
                .host(databaseInfo.getHost())
                .port(databaseInfo.getPort())
                .username(databaseInfo.getUsername())
                .password(databaseInfo.getPassword()).build();
        DbType dbType = DbType.getDbType(databaseInfo.getDbType());
        Object line;
        switch(dbType) {
            case MARIADB: case MYSQL: { // 引擎不支持mariadb
                line = new MysqlDataSourceProductionLine();
                break;
            }
            case SQL_SERVER: {
                line = new MssqlDataSourceProductionLine();
                break;
            }
            case POSTGRE_SQL: {
                line = new PostgresqlDataSourceProductionLine();
                break;
            }
            case ORACLE: {
                line = new OracleDataSourceProductionLine();
                break;
            }
            default:
                throw new DBProxyException(FlowErrorEnum.NOT_SUPPORT_TYPE);
        }
        return ((DataSourceProductionLine)line).build(material, dsConfig, tenantId);
    }
}
