package com.supcon.supfusion.flow.engine.server.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.DBProxyException;
import com.supcon.supfusion.flow.engine.server.listener.AbstractMultipleTaskStartListener;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessCompleteListener;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessStartListener;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessTerminateListener;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DatabasePopulatorProperties;
import com.supcon.supfusion.framework.boot.scaffold.dbp.MultiTenantDataSourceProperties;
import com.supcon.supfusion.framework.scaffold.dbp.factory.populator.MultiTenantDatabasePopulator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.cfg.SpringBeanFactoryProxyMap;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.multitenant.MultiSchemaMultiTenantProcessEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.multitenant.ExecutorPerTenantAsyncExecutor;
import org.flowable.spring.SpringExpressionManager;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableAutoConfiguration(exclude={ProcessEngineAutoConfiguration.class})
@EnableConfigurationProperties({
        DatabasePopulatorProperties.class,
        MultiTenantDataSourceProperties.class,
        DataSourceConnectionProperties.class})
@Configuration
@Slf4j
public class MultiSchemaProcessEngineInit {

    @Autowired
    @Lazy
    private AbstractProcessTerminateListener processTerminateListener;
    @Autowired
    @Lazy
    private AbstractProcessCompleteListener processCompleteListener;
    @Autowired
    @Lazy
    private AbstractProcessStartListener processStartListener;
    @Autowired
    @Lazy
    private AbstractMultipleTaskStartListener multipleTaskStartListener;

    @Value("${supfusion.cloud.datasource.connect.system.db-type:mysql}")
    private String dbType;
    @Value("${supfusion.cloud.datasource.connect.system.username:@null}")
    private String databaseSchema;


    @Bean
    @Primary
    public MultiTenantAwareDataSource tenantAwareDatasource(MultiTenantInfoHolder tenantInfoHolder) {
        return new MultiTenantAwareDataSource(tenantInfoHolder);
    }

    @Bean
    @Primary
    public DataId dataId(DataSourceConnectionProperties connectionProperties) {
        if (connectionProperties != null && connectionProperties.getUseSystem() && connectionProperties.getSystem() != null) {
            return new DataId(true, connectionProperties.getSystem().getDbType());
        }
        return new DataId(false, null);
    }

    @Bean
    @Primary
    public MultiTenantDatabasePopulator databasePopulator(DatabasePopulatorProperties properties) {
        return new MultiTenantDatabasePopulator(properties, properties.getScriptRootDirs());
    }

    @Bean
    @Primary
    @ConditionalOnClass(MultiTenantAwareDataSource.class)
    public MultiTenantDataSourceTransactionManager dataSourceTransactionManager(
            MultiTenantAwareDataSource multiTenantAwareDataSource, MultiTenantInfoHolder tenantInfoHolder) {
        return new MultiTenantDataSourceTransactionManager(multiTenantAwareDataSource, tenantInfoHolder);
    }

    /**
     * 多租户多库模式
     * @param tenantInfoHolder
     * @param multiTenantAwareDataSource
     * @return
     */
    @Bean
    @ConditionalOnClass(MultiTenantAwareDataSource.class)
    public SpringMultiSchemaMultiTenantProcessEngineConfiguration multiSchemaProcessEngineConfiguration(
                                        ApplicationContext applicationContext,
                                        MultiTenantInfoHolder tenantInfoHolder,
                                        PlatformTransactionManager transactionManager,
                                        MultiTenantAwareDataSource multiTenantAwareDataSource) {
        SpringMultiSchemaMultiTenantProcessEngineConfiguration cfg = new SpringMultiSchemaMultiTenantProcessEngineConfiguration(
                tenantInfoHolder, transactionManager);
        cfg.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        cfg.setAsyncExecutorActivate(true);
        cfg.setFallbackToDefaultTenant(false);
        cfg.setDisableIdmEngine(true);
        cfg.setTransactionsExternallyManaged(true);
        cfg.setCreateDiagramOnDeploy(false);
        cfg.setIdGenerator(new CustomIDGenerator());
        cfg.setAsyncExecutor(new ExecutorPerTenantAsyncExecutor(tenantInfoHolder));
        // 设置流程监听器
        Map<String, List<FlowableEventListener>> listenerMap = new HashMap<>(8);
        listenerMap.put(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT.name(), Collections.singletonList(processTerminateListener));
        listenerMap.put(FlowableEngineEventType.PROCESS_COMPLETED.name(), Collections.singletonList(processCompleteListener));
        listenerMap.put(FlowableEngineEventType.PROCESS_STARTED.name(), Collections.singletonList(processStartListener));
        listenerMap.put(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED.name(), Collections.singletonList(multipleTaskStartListener));
        DbType dbTypeEnum = DbType.getDbType(this.dbType);
        switch(dbTypeEnum) {
            case MYSQL: case MARIADB: {
                cfg.setDatabaseType(MultiSchemaMultiTenantProcessEngineConfiguration.DATABASE_TYPE_MYSQL);
                break;
            }
            case SQL_SERVER: {
                cfg.setDatabaseType(MultiSchemaMultiTenantProcessEngineConfiguration.DATABASE_TYPE_MSSQL);
                break;
            }
            case POSTGRE_SQL: {
                cfg.setDatabaseType("postgres");
                break;
            }
            case ORACLE: {
                cfg.setDatabaseType(MultiSchemaMultiTenantProcessEngineConfiguration.DATABASE_TYPE_ORACLE);
                if (StringUtils.isNotEmpty(databaseSchema)) {
                    cfg.setDatabaseSchema(databaseSchema);
                }
                break;
            }
            default:
                throw new DBProxyException(FlowErrorEnum.NOT_SUPPORT_TYPE);
        }
        cfg.setTypedEventListeners(listenerMap);
        cfg.setDataSource(multiTenantAwareDataSource);
        // bean都交给spring容器管理
        cfg.setExpressionManager(new SpringExpressionManager(applicationContext, cfg.getBeans()));
        cfg.setBeans(new SpringBeanFactoryProxyMap(applicationContext));
        return cfg;
    }

    @Bean
    public ProcessEngine processEngine(SpringMultiSchemaMultiTenantProcessEngineConfiguration cfg) {
        ProcessEngine processEngine = cfg.buildProcessEngine();
        return processEngine;
    }
}
