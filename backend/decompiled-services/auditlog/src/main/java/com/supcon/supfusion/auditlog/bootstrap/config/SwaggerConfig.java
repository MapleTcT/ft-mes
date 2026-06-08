/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  springfox.documentation.builders.ApiInfoBuilder
 *  springfox.documentation.builders.ParameterBuilder
 *  springfox.documentation.builders.PathSelectors
 *  springfox.documentation.builders.RequestHandlerSelectors
 *  springfox.documentation.schema.ModelRef
 *  springfox.documentation.schema.ModelReference
 *  springfox.documentation.service.ApiInfo
 *  springfox.documentation.service.Parameter
 *  springfox.documentation.spi.DocumentationType
 *  springfox.documentation.spring.web.plugins.Docket
 *  springfox.documentation.swagger2.annotations.EnableSwagger2
 */
package com.supcon.supfusion.auditlog.bootstrap.config;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Value(value="${supfusion.swagger.enable:false}")
    private boolean swaggerEnable;

    @Bean
    public Docket createRestApi() {
        ParameterBuilder parameterBuilder = new ParameterBuilder();
        parameterBuilder.name("Content-Type").defaultValue("application/json;chartset=UTF-8").modelRef((ModelReference)new ModelRef("string")).parameterType("header").required(true).build();
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(parameterBuilder.build());
        return new Docket(DocumentationType.SWAGGER_2).enable(this.swaggerEnable).apiInfo(this.apiInfo()).select().apis(RequestHandlerSelectors.basePackage((String)"com.supcon.supfusion.auditlog")).paths(PathSelectors.any()).build().globalOperationParameters(parameters).pathMapping("/");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("\u5ba1\u8ba1\u65e5\u5fd7\u63a5\u53e3\u6587\u6863").version("1.0.0").build();
    }
}

