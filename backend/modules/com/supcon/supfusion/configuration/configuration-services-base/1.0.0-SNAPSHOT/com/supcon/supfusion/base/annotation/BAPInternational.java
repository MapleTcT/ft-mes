/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.base.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * BAP辨别字段是否需要国际化的Annotation
 * 
 * @author tanzhengyang
 * 
 */
@Documented
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface BAPInternational {

	String fieldName() default "";

	boolean replace() default true;
}