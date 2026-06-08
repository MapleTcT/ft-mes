package com.supcon.supfusion.flow.taskcenter.listener;

import com.supcon.supfusion.flow.common.dto.TenantEventMessageDto;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.util.JsonUtil;
import com.supcon.supfusion.framework.scaffold.dbp.DataSourceConfig;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.exception.DBProxyErrorEnum;
import com.supcon.supfusion.framework.scaffold.dbp.exception.DBProxyException;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.DataSourceProductionMaterial;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MariadbDataSourceProductionLine;
import com.supcon.supfusion.framework.scaffold.dbp.factory.line.MysqlDataSourceProductionLine;
import com.supcon.supfusion.tenant.api.TenantManagerService;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component("tenantInitialListener")
@Slf4j
public class TenantInitialListener {

    @Autowired
    private DataSourceConfig dsConfig;
    @Autowired
    @Lazy
    private TenantManagerService tenantManagerService;
    @Value("${spring.application.name}")
    private String serviceName;

    // TODO 处理重复消费
    @KafkaListener(topics = {Constants.KFK_TOPIC_TENANT_EVENT}, containerFactory = "flowTenantInitContainerFactory")
    public void listen(ConsumerRecord<String, String> kafkaRecord) {
        log.info("================监听到租户创建事件, 开始初始化trigger====================");
        String sqlFile = "META-INF/custom/flow_tenant_init.sql";
        try {
            TenantEventMessageDto tenantMessage = JsonUtil.parse(kafkaRecord.value(), TenantEventMessageDto.class);
            switch (tenantMessage.getBody().getEventType()) {
                case ADD: {
                    handleCreate(tenantMessage, sqlFile);break;
                }
                case DESTROY: handleDestory();
                default: log.error("无法识别kafka消息类型: {}", tenantMessage.getBody().getEventType());
            }
        } catch (Exception ignore) {
            // ignore
        }
    }

    private void handleDestory() {
        // TODO
    }

    private void handleCreate(TenantEventMessageDto tenantMessage, String sqlFile) throws IOException, SQLException {
        ListResult<TenantDTO> tenants = tenantManagerService.find(tenantMessage.getBody().getId());
        if (tenants.getList() == null || tenants.getList().isEmpty()) {
            log.error("无法获取租户数据,租户ID={}", tenantMessage.getBody().getId());
            return;
        }
        // 取第一个租户
        TenantDTO tenant = tenants.getList().iterator().next();
        for (TenantDTO.DatabaseDTO databaseInfo : tenant.getDatabaseInfos()) {
            // 取主库
            if (databaseInfo.getMajor()) {
                DataSource dataSource = buildDataSource(databaseInfo, tenantMessage.getBody().getId());
                executeScript(dataSource, sqlFile, tenant.getId(), databaseInfo.getDbName());
            }
        }
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
            default:
                throw new DBProxyException(DBProxyErrorEnum.NOT_SUPPORT_TYPE);
        }
        return ((DataSourceProductionLine)line).build(material, this.dsConfig, tenantId);
    }

    private void executeScript(DataSource dataSource, String scriptFile, String tenantId, String dbName) throws SQLException, IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resource = null;
        Statement stat = null;
        InputStreamReader inStreamReaderUpdate = null;
        try (Connection conn = dataSource.getConnection()) {
            resource = resolver.getResources(scriptFile);
            conn.setCatalog(dbName);
            stat = conn.createStatement();
            inStreamReaderUpdate = new InputStreamReader(resource[0].getInputStream(), StandardCharsets.UTF_8);
            String sql = new String(IOUtils.toString(inStreamReaderUpdate).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).replaceAll("\n", " ");
            String[] sqlArr = sql.split(";");
            for (String sqlStr : sqlArr) {
                if (StringUtils.isNotEmpty(sqlStr.trim())) {
                    stat.execute(sqlStr);
                }
            }
        } finally {
            if (inStreamReaderUpdate != null) {
                try {
                    inStreamReaderUpdate.close();
                } catch (Exception e) {
                    log.error("关闭InputStreamReader失败", e);
                }
            }
            if (stat != null) {
                stat.close();
            }
        }
    }

}
