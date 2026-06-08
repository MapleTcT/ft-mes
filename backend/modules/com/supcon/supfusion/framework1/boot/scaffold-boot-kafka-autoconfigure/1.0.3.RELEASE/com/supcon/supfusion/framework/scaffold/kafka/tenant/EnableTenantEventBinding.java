package com.supcon.supfusion.framework.scaffold.kafka.tenant;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author tomcat
 * @date 20-6-4 上午10:59
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(TenantEventSinkBindingBeansRegistrar.class)
public @interface EnableTenantEventBinding {}
