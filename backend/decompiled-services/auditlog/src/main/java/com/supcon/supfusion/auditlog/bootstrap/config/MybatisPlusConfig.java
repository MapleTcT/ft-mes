/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.annotation.DbType
 *  com.baomidou.mybatisplus.core.parser.ISqlParser
 *  com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor
 *  com.baomidou.mybatisplus.extension.plugins.pagination.dialects.IDialect
 *  com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MariaDBDialect
 *  com.baomidou.mybatisplus.extension.plugins.pagination.dialects.OracleDialect
 *  com.baomidou.mybatisplus.extension.plugins.pagination.dialects.SQLServerDialect
 *  com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supfusion.auditlog.bootstrap.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.IDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.MariaDBDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.OracleDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.dialects.SQLServerDialect;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        paginationInterceptor.setCountSqlParser((ISqlParser)new JsqlParserCountOptimize(true));
        String dbType = System.getenv().get("db-type");
        if (DbType.ORACLE.getDb().equals(dbType)) {
            paginationInterceptor.setDbType(DbType.ORACLE);
            OracleDialect oracleDialect = new OracleDialect();
            paginationInterceptor.setDialect((IDialect)oracleDialect);
        } else if (DbType.MARIADB.getDb().equals(dbType)) {
            paginationInterceptor.setDbType(DbType.MARIADB);
            MariaDBDialect mariaDBDialect = new MariaDBDialect();
            paginationInterceptor.setDialect((IDialect)mariaDBDialect);
        } else if (DbType.SQL_SERVER.getDb().equals(dbType)) {
            paginationInterceptor.setDbType(DbType.SQL_SERVER);
            SQLServerDialect sqlServerDialect = new SQLServerDialect();
            paginationInterceptor.setDialect((IDialect)sqlServerDialect);
        }
        return paginationInterceptor;
    }
}

