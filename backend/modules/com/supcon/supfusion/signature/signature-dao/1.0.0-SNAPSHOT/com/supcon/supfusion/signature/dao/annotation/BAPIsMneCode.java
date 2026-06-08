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
 * BAP辨别字段是否用于助记码的Annotation
 * @author fangzhibin
 *
 */
@Documented
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)
public @interface BAPIsMneCode {

}
