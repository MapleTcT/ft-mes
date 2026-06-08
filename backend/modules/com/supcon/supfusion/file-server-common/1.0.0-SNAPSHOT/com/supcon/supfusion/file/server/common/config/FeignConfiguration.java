/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.file.server.common.config;

import feign.Feign;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;

/**
 * @author:
 * @date: 2020年10月12日 上午11:45:27
 */
@Configuration
@ConditionalOnClass({Feign.class})
public class FeignConfiguration {
    @Bean
    public WebMvcRegistrations feignWebRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new FeignRequestMappingHandlerMapping();
            }
        };
    }
    
    // 过滤FeignClient产生的request mapping注册到上下文
    private class FeignRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
        @Override
        protected boolean isHandler(Class<?> beanType) {
            return super.isHandler(beanType) &&
                    !containFeignClientAnnotation(beanType);
        }
        
        private boolean containFeignClientAnnotation(Class<?> beanType) {
            Annotation[] annotations = beanType.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof FeignClient) {
                    return true;
                }
            }
            return false;
        }
    }
}
