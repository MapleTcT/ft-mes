/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BAP辨别字段是否需要国际化的Annotation
 * 
 * @author tanzhengyang
 * 
 */
@Target(TYPE) 
@Retention(RUNTIME)
public @interface BAPClassInternational {

	String[] fieldNames()  default {};

}