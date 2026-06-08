package com.supcon.supfusion.signature.base.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

@Configuration()
@EnableSwagger2
public class SwaggerConfig {
    @Value(value = "${supfusion.swagger.enable:false}")
    private boolean swaggerEnable;

    /**
     * 创建一个Docket 并且注册到Spring容器里即可完成配置
     *
     * @return Docket
     */
    @Primary
    @Bean
    public Docket createRestApi() {
        ParameterBuilder parameterBuilder = new ParameterBuilder();
        parameterBuilder.name("Content-Type").defaultValue("application/json;chartset=UTF-8").modelRef(new ModelRef("string")).parameterType("header").required(true).build();
        List<Parameter> parameters = new ArrayList<>();
        parameters.add(parameterBuilder.build());

        return new Docket(DocumentationType.SWAGGER_2)
                //是否开启
                .enable(swaggerEnable)
                //设置API描述信息
                .apiInfo(apiInfo())
                .select()
                //扫描的的包 一般定位到controller那即可（若有接口层，此处报名需要注意）
                .apis(RequestHandlerSelectors.basePackage("com.supcon.supfusion.signature"))
                // 指定路径处理：PathSelectors.any()代表所有的路径
                // 也可以指定某些接口不要对外暴露 这里定义一些规则就行 如正则表达式
                .paths(PathSelectors.any())
                .build().globalOperationParameters(parameters)
                //base，最终调用接口后会和paths拼接在一起
                .pathMapping("/");
    }

    //设置API信息 一些简单的描述信息而已
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("电子签名接口")
                .description("API描述信息")
                .version("1.0.0")
                .build();
    }

}
