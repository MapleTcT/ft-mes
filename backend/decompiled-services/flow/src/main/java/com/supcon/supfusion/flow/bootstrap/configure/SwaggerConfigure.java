/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  springfox.documentation.builders.ApiInfoBuilder
 *  springfox.documentation.builders.PathSelectors
 *  springfox.documentation.builders.RequestHandlerSelectors
 *  springfox.documentation.service.ApiInfo
 *  springfox.documentation.service.Contact
 *  springfox.documentation.spi.DocumentationType
 *  springfox.documentation.spring.web.plugins.Docket
 *  springfox.documentation.swagger.web.OperationsSorter
 *  springfox.documentation.swagger.web.UiConfiguration
 *  springfox.documentation.swagger.web.UiConfigurationBuilder
 */
package com.supcon.supfusion.flow.bootstrap.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.OperationsSorter;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;

@Configuration
public class SwaggerConfigure {
    @Bean
    public Docket createFlowRestApi() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("flow-webapi").apiInfo(this.flowApiInfo()).select().apis(RequestHandlerSelectors.basePackage((String)"com.supcon.supfusion.flow.webapi")).paths(PathSelectors.any()).build();
    }

    @Bean
    public Docket createFlowOpenApi() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("flow-openapi").apiInfo(this.flowApiInfo()).select().apis(RequestHandlerSelectors.basePackage((String)"com.supcon.supfusion.flow.openapi")).paths(PathSelectors.any()).build();
    }

    @Bean
    public UiConfiguration flowUIConfig() {
        return UiConfigurationBuilder.builder().operationsSorter(OperationsSorter.METHOD).build();
    }

    private ApiInfo flowApiInfo() {
        return new ApiInfoBuilder().title("\u5de5\u4f5c\u6d41\u540e\u7aefapi\u63a5\u53e3\u6587\u6863").contact(new Contact("\u5e84\u660e\u6cb3", "http://www.supOS.com", "zhuangmh@supos.com")).description("\u5de5\u4f5c\u6d41\u540e\u7aefapi\u63a5\u53e3\u6587\u6863").version("1.0").build();
    }
}

