/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.supcon.supfusion.flow.engine.server.listener.AbstractMultipleTaskStartListener;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessCompleteListener;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessStartListener;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessTerminateListener;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;

/**
 * @author: zhuangmh
 * @date: 2020年5月28日 下午1:37:35
 */
@Configuration
public class FlowableConfigurator implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

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
    
    @Value("${supfusion.cloud.datasource.connect.system.db-type:mariadb}")
    private String dbType;
    @Value("${supfusion.cloud.datasource.connect.system.username:@null}")
    private String databaseSchema;
    
    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setCreateDiagramOnDeploy(false);
        engineConfiguration.setIdGenerator(new CustomIDGenerator());
        engineConfiguration.setAsyncExecutorActivate(true); // 开启异步执行executor
        // 设置流程监听器
        Map<String, List<FlowableEventListener>> listenerMap = new HashMap<>(8);
        listenerMap.put(FlowableEngineEventType.PROCESS_COMPLETED_WITH_TERMINATE_END_EVENT.name(), Collections.singletonList(processTerminateListener));
        listenerMap.put(FlowableEngineEventType.PROCESS_COMPLETED.name(), Collections.singletonList(processCompleteListener));
        listenerMap.put(FlowableEngineEventType.PROCESS_STARTED.name(), Collections.singletonList(processStartListener));
        listenerMap.put(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED.name(), Collections.singletonList(multipleTaskStartListener));
        if (DbType.getDbType(dbType) == DbType.ORACLE
                && StringUtils.isNotEmpty(databaseSchema)) {
            engineConfiguration.setDatabaseSchema(databaseSchema);
        }
        engineConfiguration.setTypedEventListeners(listenerMap);
    }
}
