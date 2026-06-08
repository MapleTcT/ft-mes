/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.druid.pool.DruidDataSource
 *  com.alibaba.fastjson.JSONObject
 *  com.supcon.supfusion.notification.sms.client.SuposClint
 *  com.supcon.supfusion.notification.sms.service.runner.Configs
 *  com.supcon.supfusion.notification.sms.service.runner.Configs$Config
 *  org.apache.commons.lang3.StringUtils
 *  org.mybatis.spring.annotation.MapperScan
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supfusion.notification.sms.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.notification.sms.client.SuposClint;
import com.supcon.supfusion.notification.sms.service.runner.Configs;
import java.util.Optional;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(value={"com.supcon.supfusion.notification.sms.dao.mappers"})
public class DataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    @Autowired
    private SuposClint suposClint;
    @Value(value="${supfusion.supos.appId:605fd270fb5b3563411572db01634214}")
    private String appId;

    @Bean
    public DataSource getDataSource() {
        String url = null;
        String userName = null;
        String password = null;
        String systemConfig = this.suposClint.getSystemConfig(this.appId);
        Configs parse = (Configs)JSONObject.parseObject((String)systemConfig, Configs.class);
        if (Optional.ofNullable(parse.getData()).isPresent()) {
            for (Configs.Config config : parse.getData().getConfig()) {
                switch (config.getCode()) {
                    case "url": {
                        url = StringUtils.strip((String)config.getValue().toString(), (String)"[]");
                        break;
                    }
                    case "name": {
                        userName = StringUtils.strip((String)config.getValue().toString(), (String)"[]");
                        break;
                    }
                    case "password": {
                        password = StringUtils.strip((String)config.getValue().toString(), (String)"[]");
                    }
                }
            }
        }
        DruidDataSource source = new DruidDataSource();
        if (null != url && null != userName && null != password) {
            log.info("=======================================================================================================================");
            log.info("= dbType={}", (Object)"sqlserver");
            log.info("= driver={}", (Object)"com.microsoft.sqlserver.jdbc.SQLServerDriver");
            log.info("= url={}", (Object)url);
            log.info("= username={}", (Object)userName);
            log.info("= password=********");
            log.info("=======================================================================================================================");
            source.setPassword(password);
            source.setUsername(userName);
            source.setUrl(url);
            source.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        }
        return source;
    }
}

