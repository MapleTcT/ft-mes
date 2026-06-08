/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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