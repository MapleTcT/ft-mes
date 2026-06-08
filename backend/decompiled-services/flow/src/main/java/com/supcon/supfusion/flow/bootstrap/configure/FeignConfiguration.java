/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  feign.Feign
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnClass
 *  org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations
 *  org.springframework.cloud.openfeign.FeignClient
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 */
package com.supcon.supfusion.flow.bootstrap.configure;

import feign.Feign;
import java.lang.annotation.Annotation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@ConditionalOnClass(value={Feign.class})
public class FeignConfiguration {
    @Bean
    public WebMvcRegistrations feignWebRegistrations() {
        return new WebMvcRegistrations(){

            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new FeignRequestMappingHandlerMapping();
            }
        };
    }

    private class FeignRequestMappingHandlerMapping
    extends RequestMappingHandlerMapping {
        private FeignRequestMappingHandlerMapping() {
        }

        protected boolean isHandler(Class<?> beanType) {
            return super.isHandler(beanType) && !this.containFeignClientAnnotation(beanType);
        }

        private boolean containFeignClientAnnotation(Class<?> beanType) {
            Annotation[] annotations;
            for (Annotation annotation : annotations = beanType.getAnnotations()) {
                if (!(annotation instanceof FeignClient)) continue;
                return true;
            }
            return false;
        }
    }
}

