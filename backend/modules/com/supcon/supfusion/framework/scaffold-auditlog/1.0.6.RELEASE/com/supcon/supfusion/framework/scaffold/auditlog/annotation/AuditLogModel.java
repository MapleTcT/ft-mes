package com.supcon.supfusion.framework.scaffold.auditlog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通过该注解获取审计日志模型信息
 * 用于实体类上
 * @author caokele
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogModel {
    /**
     * 模型编码
     */
    String modelCode();
    /**
     * 模型名称(国际化key)
     */
    String modelName();
    /**
     * 实体编码
     */
    String entityCode() default "";
    /**
     * 实体名称(国际化key)
     */
    String entityName() default "";
}
