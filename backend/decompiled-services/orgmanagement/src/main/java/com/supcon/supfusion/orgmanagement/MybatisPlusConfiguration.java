/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.parser.ISqlParser
 *  com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor
 *  com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize
 *  com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supfusion.orgmanagement;

import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfiguration {
    @Autowired
    private DataSourceConnectionProperties dataSourceConnectionProperties;

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor().setCountSqlParser((ISqlParser)new JsqlParserCountOptimize(true));
    }
}

