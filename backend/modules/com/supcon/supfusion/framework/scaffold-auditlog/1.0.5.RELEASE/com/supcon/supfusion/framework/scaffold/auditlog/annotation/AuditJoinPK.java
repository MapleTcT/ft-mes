package com.supcon.supfusion.framework.scaffold.auditlog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识连接字段的主键名
 * @author caokele
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditJoinPK {
    /**
     * 主键字段名称
     */
    String value();
}
