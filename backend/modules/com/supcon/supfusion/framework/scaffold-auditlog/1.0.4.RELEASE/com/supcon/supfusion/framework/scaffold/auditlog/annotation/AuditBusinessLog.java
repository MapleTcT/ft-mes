package com.supcon.supfusion.framework.scaffold.auditlog.annotation;


import com.supcon.supfusion.framework.scaffold.auditlog.constant.OperateType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务审计日志注解
 * 用于在方法上组装业务日志
 *
 * @author caokele
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditBusinessLog {
    /**
     * 模块名称（国际化key）
     */
    String moduleName();

    /**
     * 模块编码
     */
    String moduleCode();

    /**
     * 操作类型
     */
    OperateType operateType();

    /**
     * 模型编码
     */
    String[] modelCodes();

    /**
     * 主模型编码
     */
    String mainModelCode() default "";

    /**
     * 操作描述(国际化key)
     */
    String desc() default "";
}
