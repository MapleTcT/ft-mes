package com.supcon.supfusion.notification.admin.service.scheduling;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantDatabaseInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantEventTypeEnum;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeProtocolMapper;
import com.supcon.supfusion.notification.sharding.intercept.DynamicTableIntercept;
import com.supcon.supfusion.notification.sharding.util.ITableNameStrategy;
import com.supcon.supfusion.tenant.api.TenantManagerService;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.tools.ant.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
@Slf4j
public class DynamicTableTask implements CommandLineRunner {

    @Autowired
    private TenantManagerService tenantManagerService;
    @Autowired
    private DataSourceConnectionProperties dataSourceConnectionProperties;
    @Resource(name = "adminNoticeProtocolMapper")
    private NoticeProtocolMapper noticeProtocolMapper;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static String alterSql = "ALTER TABLE %s ADD %s %s";

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());

    /**
     * 创建次月的分表，作用与服务运行时动态建表
     */
    @Scheduled(cron = "0 0 0 2 * ?")
    public void schedule() {
        try {
            createTable(1,null);
        } catch (Exception e) {
            log.error("====================定时任务建表失败======================");
        }
    }

    /**
     * 创建上一个月、当月和次月的分表，作用于服务初次部署或者重新启动时动态建表
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) {
        try {
            createTable(-1,null);
            createTable(0,null);
            createTable(1,null);
        } catch (Exception e) {
            log.error("====================notification-admin启动任务建表失败======================");
//            System.exit(1);
        }
        try {
            //notice_msg 在指定版本(包含)以前的版本添加字段
            alterTable("1.0.2");
        } catch (Exception e) {
            log.error("====================notification-admin启动任务notice_msg表添加字段失败======================");
        }

    }

    /**
     * 获取所有租户数据源，notice_task按照月份分表，notice_msg按照协议和月份分表。
     *
     * @param offset true 创建下一个月的分表，作用与服务运行时动态建表；false 创建当月的分表，作用于服务初次部署或者重新启动时动态建表。
     */
    public void createTable(int offset, @Nullable String tenantids) throws Exception {
        try {
            Boolean useSystem = dataSourceConnectionProperties.getUseSystem();
            log.info("dynamic create table.....useSystem({}) for {}", useSystem,tenantids);
            if (useSystem == null || !useSystem) {
                boolean created = useTenant(offset,tenantids);
                if (!created){
                    scheduledExecutor.schedule(
                            ()-> {
                                try {
                                    createTable(offset,tenantids);
                                } catch (Exception e) {
                                }
                            }
                            ,60, TimeUnit.SECONDS);
                }
            } else {
                useSystem(offset);
            }
        } catch (SQLSyntaxErrorException e) {
            //表已存在，捕获异常
            if ("42S02".equals(e.getSQLState()) || "42000".equals(e.getSQLState())) {
                log.error(e.getMessage(), e);
            } else {
                throw e;
            }
        } catch (SQLServerException e) {
            //表已存在，捕获异常
            if ("S0002".equals(e.getSQLState())) {
                log.error(e.getMessage(), e);
            } else {
                throw e;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private void useSystem(int offset) throws Exception {
        Set<TenantInfo> tenantInfos = TenantInfoLocalStorage.getAll();
        if (tenantInfos == null || tenantInfos.isEmpty()) {
            log.info("there is no tenant");
        }

        for (TenantInfo tenantInfo : tenantInfos) {
            String tenantId = tenantInfo.getId();
            String dbType = tenantInfo.getDatabaseInfo().getDbType();
            if (dbType == null || "".equals(dbType)) {
                log.info("there is no datasource in {}", tenantId);
                continue;
            }

            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            log.info("get {} datasource", tenantId);

            List<NoticeProtocol> noticeProtocols = noticeProtocolMapper.selectList(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getValidFieldName(), 1));
            if (noticeProtocols == null || noticeProtocols.isEmpty()) {
                log.info("there is no protocol");
                continue;
            }
            execSql(offset, tenantId, dbType, noticeProtocols);
        }

    }

    private boolean useTenant(int offset,String tenantIds) throws Exception {
        ListResult<TenantDTO> tenantDTOListResult = tenantManagerService.find(tenantIds);
        Collection<TenantDTO> tenantDTOS = tenantDTOListResult.getList();
        if (tenantDTOS == null || tenantDTOS.size() == 0) {
            log.info("there is no tenant,will loop it ");
            return false ;
        }

        for (TenantDTO tenantDTO : tenantDTOS) {
            String tenantId = tenantDTO.getId();
            String dbType = "";
            /**
             *　全量初始化租户数据源，防止数据源缺失
             */
            for (TenantDTO.DatabaseDTO databaseDTO : tenantDTO.getDatabaseInfos()) {
                log.info("get databaseDTO: {} ", databaseDTO.toString());
                if (databaseDTO.getMajor() == null || !databaseDTO.getMajor()) {
                    continue;
                }
                dbType = databaseDTO.getDbType();
                TenantDatabaseInfo tenantDatabaseInfo = new TenantDatabaseInfo(databaseDTO.getHost(), databaseDTO.getPort(), databaseDTO.getUsername(), databaseDTO.getPassword(), databaseDTO.getDbName(), databaseDTO.getDbType(), null);
                TenantInfo tenantInfo = new TenantInfo(TenantEventTypeEnum.ADD, tenantDTO.getId(), tenantDTO.getInstanceId(), tenantDTO.getDescription(), tenantDatabaseInfo);
                log.info("init {} datasource, tenantInfo:{}", tenantId, tenantInfo.toString());
                TenantInfoLocalStorage.add(tenantInfo);
            }
            if (dbType == null || "".equals(dbType)) {
                log.info("there is no datasource in {}", tenantId);
                continue;
            }

            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            log.info("get {} datasource", tenantId);

            List<NoticeProtocol> noticeProtocols = noticeProtocolMapper.selectList(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getValidFieldName(), 1));
            if (noticeProtocols == null || noticeProtocols.size() == 0) {
                log.info("there is no protocol");
                continue;
            }
            execSql(offset, tenantId, dbType, noticeProtocols);
        }

        return true;
    }

    private void execSql(int offset, String tenantId, String dbType, List<NoticeProtocol> noticeProtocols) throws Exception {
        /**
         * 执行sql脚本，创建表
         */
        Connection connection = dataSource.getConnection();
        StringBuilder sqlScript = new StringBuilder();
        try {
            String date = ITableNameStrategy.getDate(offset);
            Map<String, String> param = new HashMap<>();
            param.put("notice_task", "notice_task".concat("_").concat(date));
            sqlScript.append(getSql("notice_task.sql.ftl", param, dbType));
            for (NoticeProtocol noticeProtocol : noticeProtocols) {
                String protocol = noticeProtocol.getProtocol();
                param = new HashMap<>();
                param.put("notice_msg", "notice_msg".concat("_").concat(protocol).concat(date));
                sqlScript.append(getSql("notice_msg.sql.ftl", param, dbType));
            }
            log.info("prepare execute sql {}, datatsource {}", sqlScript.toString(), tenantId);
            String sql = sqlScript.toString();
            int num = sql.lastIndexOf(';');
            String finalSql = sql.substring(0, num);
            String[] sqls = finalSql.split(";");
            for (String exSql : sqls) {
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(exSql);
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    log.warn("创建表失败:{}", exSql);
                }
            }
        } finally {
            connection.close();
        }
    }


    //notice_msg 在指定版本(包含)以前的版本添加字段
    private void alterTable(String version) {
        try {
            Boolean useSystem = dataSourceConnectionProperties.getUseSystem();
            log.info("dynamic alter table.....useSystem({})", useSystem);
            List<String> times = getTimes(version);
            if (useSystem == null || !useSystem) {
                //租户
                useTenantAlterTable(times);
            } else {
                //系统用户
                useSystemAlterTable(times);
            }
        } catch (Exception e) {
            log.error("====================notification-admin启动任务notice_msg表添加字段失败======================");
            log.error(e.getMessage());
        }

    }

    private List<String> getTimes(String version) throws Exception {
        String sql = "select application_name,current_version,create_time from sys_scripts_version ssv where application_name like '%notification%' and  current_version <= ?";
        List<HashMap<String, String>> query = jdbcTemplate.query(sql, new Object[]{version}, (rs, rowNum) -> {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("applicationName", rs.getString("application_name"));
            hashMap.put("currentVersion", rs.getString("current_version"));
            hashMap.put("createTime", rs.getString("create_time"));
            return hashMap;
        });

        if (query == null || query.get(0) == null || StringUtils.isEmpty(query.get(0).get("createTime"))) {
            return null;
        }
        String createTime = query.get(0).get("createTime").substring(0, 7).replace("-", "");
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 1; i > -24; i--) {
            String date = getDate(i);
            strings.add(date);
            if (createTime.equals(date)) {
                strings.add(getDate(i - 1));
                return strings;
            }
        }
        return strings;
    }

    private void useSystemAlterTable(List<String> times) throws SQLException {
        Set<TenantInfo> tenantInfos = TenantInfoLocalStorage.getAll();
        if (tenantInfos == null || tenantInfos.isEmpty()) {
            log.info("there is no tenant");
        }

        for (TenantInfo tenantInfo : tenantInfos) {
            String tenantId = tenantInfo.getId();
            String dbType = tenantInfo.getDatabaseInfo().getDbType();
            if (dbType == null || "".equals(dbType)) {
                log.info("there is no datasource in {}", tenantId);
                continue;
            }

            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            log.info("get {} datasource", tenantId);
            //添加数据库字段
            execalterTableSql(tenantId, dbType, times);
        }
    }

    private void useTenantAlterTable(List<String> times) throws SQLException {
        ListResult<TenantDTO> tenantDTOListResult = tenantManagerService.find(null);
        Collection<TenantDTO> tenantDTOS = tenantDTOListResult.getList();
        if (tenantDTOS == null || tenantDTOS.size() == 0) {
            log.info("there is no tenant");
            return;
        }

        for (TenantDTO tenantDTO : tenantDTOS) {
            String tenantId = tenantDTO.getId();
            String dbType = "";
            /**
             *　全量初始化租户数据源，防止数据源缺失
             */
            for (TenantDTO.DatabaseDTO databaseDTO : tenantDTO.getDatabaseInfos()) {
                log.info("get databaseDTO: {} ", databaseDTO.toString());
                if (databaseDTO.getMajor() == null || !databaseDTO.getMajor()) {
                    continue;
                }
                dbType = databaseDTO.getDbType();
                TenantDatabaseInfo tenantDatabaseInfo = new TenantDatabaseInfo(databaseDTO.getHost(), databaseDTO.getPort(), databaseDTO.getUsername(), databaseDTO.getPassword(), databaseDTO.getDbName(), databaseDTO.getDbType(), null);
                TenantInfo tenantInfo = new TenantInfo(TenantEventTypeEnum.ADD, tenantDTO.getId(), tenantDTO.getInstanceId(), tenantDTO.getDescription(), tenantDatabaseInfo);
                log.info("init {} datasource, tenantInfo:{}", tenantId, tenantInfo.toString());
                TenantInfoLocalStorage.add(tenantInfo);
            }
            if (dbType == null || "".equals(dbType)) {
                log.info("there is no datasource in {}", tenantId);
                continue;
            }

            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setTenantId(tenantId);
            log.info("get {} datasource", tenantId);

            execalterTableSql(tenantId, dbType, times);

        }

    }

    private void execalterTableSql(String tenantId, String dbType, List<String> dates) throws SQLException {
        List<NoticeProtocol> noticeProtocols = noticeProtocolMapper.selectList(Wrappers.<NoticeProtocol>query().eq(NoticeProtocol.getValidFieldName(), 1));
        if (noticeProtocols == null || noticeProtocols.size() == 0) {
            log.info("there is no protocol");
            return;
        }

        /**
         * 执行sql脚本，创建表
         */
        Connection connection = dataSource.getConnection();
        String topicId = "topic_id";
        String param = "param";
        try {
            ArrayList<String> sqls = new ArrayList<>();
            for (String date : dates) {
                noticeProtocols.forEach(noticeProtocol -> {
                    String tableName = "notice_msg".concat("_").concat(noticeProtocol.getProtocol()).concat(date);
                    switch (dbType) {
                        case "oracle":
                            sqls.add(String.format(alterSql, tableName, topicId, "NUMBER(20,0)"));
                            sqls.add(String.format(alterSql, tableName, param, "CLOB"));
                            break;
                        case "sqlserver":
                            sqls.add(String.format(alterSql, tableName, topicId, "BIGINT(20)"));
                            sqls.add(String.format(alterSql, tableName, param, "LONGTEXT"));
                            break;
                        case "mysql":
                        case "mariadb":
                            sqls.add(String.format(alterSql, tableName, topicId, "bigint"));
                            sqls.add(String.format(alterSql, tableName, param, "text"));
                            break;
                        default:
                    }
                });
            }
            for (String exSql : sqls) {
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(exSql);
                    preparedStatement.executeUpdate();
                } catch (Exception e) {
                    log.error("创建表失败:{}", exSql);
                }
            }
        } finally {
            connection.close();
        }

    }


    /**
     * 第三方通知APP注册时，动态添加对应协议的分表
     * xxx
     *
     * @param protocol
     */
    public void createTable(String protocol) throws Exception {
        PreparedStatement preparedStatement = null;
        try (Connection connection = dataSource.getConnection()) {
            String tenantId = RpcContext.getContext().getTenantId();
            if (StringUtils.isEmpty(tenantId)) {
                tenantId = "system001";
            }
            TenantInfo tenantInfo = TenantInfoLocalStorage.get(tenantId);
            if (tenantInfo == null) {
                tenantInfo = TenantInfoLocalStorage.get("system001");
            }
            String dbType = tenantInfo.getDatabaseInfo().getDbType();
            StringBuilder sqlScript = new StringBuilder();
            /**
             * 创建当月的分表
             */
            String date = ITableNameStrategy.getDate(0);
            Map<String, String> param = new HashMap<>();
            param.put("notice_msg", "notice_msg".concat("_").concat(protocol).concat(date));
            sqlScript.append(getSql("notice_msg.sql.ftl", param, dbType));

            /**
             * 创建下一个月的分表
             */
            date = ITableNameStrategy.getDate(1);
            param = new HashMap<>();
            param.put("notice_msg", "notice_msg".concat("_").concat(protocol).concat(date));
            sqlScript.append(getSql("notice_msg.sql.ftl", param, dbType));

            /**
             * 创建上一个月的分表，防止1号12点以前数据双写时找不到上一个月的表
             */
            date = ITableNameStrategy.getDate(-1);
            param = new HashMap<>();
            param.put("notice_msg", "notice_msg".concat("_").concat(protocol).concat(date));
            sqlScript.append(getSql("notice_msg.sql.ftl", param, dbType));

            log.info("prepare execute sql {}, datatsource {}", sqlScript.toString(), tenantId);
            String sqlScriptStr = sqlScript.toString();
            int num = sqlScriptStr.lastIndexOf(';');
            String finalSql = sqlScriptStr.substring(0, num);
            String[] sqls = finalSql.split(";");
            for (String sql : sqls) {
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.executeUpdate();
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }


    /**
     * 输出sql脚本
     *
     * @param sqlScript 模板名称
     * @param param     数据
     */
    private String getSql(String sqlScript, Map<String, String> param, String dbType) throws Exception {
        try (StringWriter stringWriter = new StringWriter();) {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
            URL resourceURL = DynamicTableIntercept.class.getClassLoader().getResource("META-INF/" + dbType + "/dynamic");
            if ("file".equalsIgnoreCase(resourceURL.getProtocol())) {
                cfg.setDirectoryForTemplateLoading(new File(resourceURL.getPath()));
            } else {
                cfg.setTemplateLoader(new ClassTemplateLoader(DynamicTableIntercept.class.getClassLoader(), "META-INF/" + dbType + "/dynamic"));
            }
            cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_28));
            Template template = cfg.getTemplate(sqlScript);

            //生成文件
            template.process(param, stringWriter);
            return stringWriter.toString();
        }
    }

    public String getDate(int offset) {
        LocalDateTime localDateTime = LocalDateTime.now();
        int year = localDateTime.getYear();
        int month = localDateTime.getMonth().getValue() + offset;
        if (offset > 0) {
            if (month > 12) {
                return String.format("%d%02d", year + 1, month - 12);
            }
        } else {
            if (month < 1) {
                return String.format("%d%02d", year + 1, month + 12);
            }
        }
        return String.format("%d%02d", year, month);
    }

}