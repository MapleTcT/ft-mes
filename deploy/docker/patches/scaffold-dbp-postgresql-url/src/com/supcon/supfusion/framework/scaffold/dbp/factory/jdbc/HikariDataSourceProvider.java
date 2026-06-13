package com.supcon.supfusion.framework.scaffold.dbp.factory.jdbc;

import com.supcon.supfusion.framework.scaffold.dbp.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class HikariDataSourceProvider implements DataSourceProvider {
    private static final Logger log = LoggerFactory.getLogger(HikariDataSourceProvider.class);
    private static final long VALIDATED_TIMEOUT = 5000L;

    @Override
    public DataSource create(
            String dbType,
            String driver,
            String url,
            String username,
            String password,
            DataSourceConfig config,
            String tenantId
    ) {
        log.info("=======================================================================================================================");
        log.info("= dbType={}", dbType);
        log.info("= driver={}", driver);
        log.info("= url={}", url);
        log.info("= username={}", username);
        log.info("= password=********");
        log.info("= initPoolSize={}", config.getInitPoolSize());
        log.info("= minPoolSize={}", config.getMinPoolSize());
        log.info("= maxPoolSize={}", config.getMaxPoolSize());
        log.info("=======================================================================================================================");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setDriverClassName(driver);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setMinimumIdle(config.getMinPoolSize());
        hikariConfig.setIdleTimeout(config.getIdleLiveMillis());
        hikariConfig.setMaxLifetime(config.getMaxLiveMillis());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setLeakDetectionThreshold(config.getIdleLiveMillis());
        hikariConfig.setValidationTimeout(
                config.getConnectionTimeout() <= VALIDATED_TIMEOUT
                        ? config.getConnectionTimeout() - 1L
                        : VALIDATED_TIMEOUT
        );
        hikariConfig.setPoolName("hikaricp-" + tenantId);
        hikariConfig.setConnectionTestQuery(getValidationQuerySQL(dbType));
        hikariConfig.setRegisterMbeans(true);
        if ("postgresql".equalsIgnoreCase(dbType) || url.startsWith("jdbc:postgresql:")) {
            hikariConfig.setAutoCommit(false);
        }

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        try {
            Connection connection = dataSource.getConnection();
            dataSource.evictConnection(connection);
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to initialize datasource", error);
        }
        return dataSource;
    }

    public DataSource create(
            String dbType,
            String driver,
            String url,
            String username,
            String password,
            DataSourceConfig config,
            String tenantId,
            String dbVersion
    ) {
        return create(dbType, driver, url, username, password, config, tenantId);
    }

    @Override
    public String getName() {
        return "HikariCP";
    }
}
