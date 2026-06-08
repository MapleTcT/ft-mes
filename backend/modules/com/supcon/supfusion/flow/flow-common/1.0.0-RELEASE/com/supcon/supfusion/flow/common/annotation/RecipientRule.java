/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.supcon.supfusion.flow.common.enumeration.RecipientSelection;

/**
 * @author: zhuangmh
 * @date: 2020年9月22日 下午3:37:11
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface RecipientRule {
    /**
     * 取值范围参考: {@link com.supcon.supfusion.flow.common.enumeration.RecipientSelection}
     * @return
     */
    RecipientSelection value();
}
