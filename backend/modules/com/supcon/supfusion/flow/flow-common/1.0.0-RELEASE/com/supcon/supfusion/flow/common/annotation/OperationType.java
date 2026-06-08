/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;

/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:42:36
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface OperationType {
    
    OperationTypeEnum[] value() default {};
}
