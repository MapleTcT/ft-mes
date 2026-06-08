/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.parser.ISqlParser
 *  com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor
 *  com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize
 *  com.fasterxml.jackson.databind.SerializationFeature
 *  org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 */
package com.supcon.supfusion.scheduler.config;

import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.supcon.supfusion.scheduler.Interceptor.MyInterceptor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor().setCountSqlParser((ISqlParser)new JsqlParserCountOptimize(true));
    }

    @Bean
    public MyInterceptor myInterceptor() {
        MyInterceptor sql = new MyInterceptor();
        return sql;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder.featuresToEnable(new Object[]{SerializationFeature.WRITE_ENUMS_USING_TO_STRING});
    }
}

