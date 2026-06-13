package com.supcon.orchid.container.orm.config;

import com.alibaba.druid.DruidRuntimeException;
import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({DataSourceDruidProperties.class, DataSourceConnectionProperties.class})
public class DataSourceConfig {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataSourceDruidProperties druidProperties;

    @Autowired
    private DataSourceConnectionProperties connectionProperties;

    @Value("${druid.monitor.enabled:false}")
    private Boolean monitorEnabled;

    @Bean
    public DataSource getDataSource() {
        String dbType = connectionProperties.getDbType();
        if (dbType == null || dbType.trim().isEmpty()) {
            logger.error("异常退出：{}", connectionProperties);
            System.exit(0);
            throw new DruidRuntimeException("Can not support empty db type!");
        }

        String url;
        String driverClassName;
        String validationQuery;

        switch (dbType.toLowerCase()) {
            case "oracle":
                driverClassName = "oracle.jdbc.OracleDriver";
                url = String.format(
                        "jdbc:oracle:thin:@%s:%d:%s",
                        connectionProperties.getHost(),
                        connectionProperties.getPort(),
                        connectionProperties.getDbName());
                validationQuery = "select 'x' FROM dual";
                break;
            case "sqlserver":
                driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                url = String.format(
                        "jdbc:sqlserver://%s:%d;databaseName=%s",
                        connectionProperties.getHost(),
                        connectionProperties.getPort(),
                        connectionProperties.getDbName());
                validationQuery = "select 'x'";
                break;
            case "mysql":
                driverClassName = "com.mysql.cj.jdbc.Driver";
                url = String.format(
                        "jdbc:mysql://%s:%d/%s",
                        connectionProperties.getHost(),
                        connectionProperties.getPort(),
                        connectionProperties.getDbName());
                validationQuery = "select 'x'";
                break;
            case "mariadb":
                driverClassName = "org.mariadb.jdbc.Driver";
                url = String.format(
                        "jdbc:mysql://%s:%d/%s",
                        connectionProperties.getHost(),
                        connectionProperties.getPort(),
                        connectionProperties.getDbName());
                validationQuery = "select 'x'";
                break;
            case "postgres":
            case "postgresql":
                driverClassName = "org.postgresql.Driver";
                url = String.format(
                        "jdbc:postgresql://%s:%d/%s",
                        connectionProperties.getHost(),
                        connectionProperties.getPort(),
                        connectionProperties.getDbName());
                validationQuery = "SELECT 1";
                break;
            default:
                throw new DruidRuntimeException("Can not support db type!");
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(connectionProperties.getUsername());
        dataSource.setPassword(connectionProperties.getPassword());
        dataSource.setDriverClassName(driverClassName);
        dataSource.setValidationQuery(validationQuery);
        if ("postgres".equalsIgnoreCase(dbType) || "postgresql".equalsIgnoreCase(dbType)) {
            dataSource.setDefaultAutoCommit(false);
        }
        dataSource.setInitialSize(druidProperties.getInitPoolSize());
        dataSource.setMaxActive(druidProperties.getMaxPoolSize());
        dataSource.setMaxWait(druidProperties.getSlowSqlMillis());
        dataSource.setMinIdle(druidProperties.getMinPoolSize());
        dataSource.setMaxOpenPreparedStatements(-1);

        if (Boolean.TRUE.equals(monitorEnabled)) {
            dataSource.setRemoveAbandoned(Boolean.TRUE.equals(druidProperties.getRemoveAbandoned()));
            dataSource.setRemoveAbandonedTimeout(druidProperties.getRemoveAbandonedTimeout());
            dataSource.setLogAbandoned(Boolean.TRUE.equals(druidProperties.getLogAbandoned()));
            try {
                dataSource.setFilters("stat");
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return dataSource;
    }
}
