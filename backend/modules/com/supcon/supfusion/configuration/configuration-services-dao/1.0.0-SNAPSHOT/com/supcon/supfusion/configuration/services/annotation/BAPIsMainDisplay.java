/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BAP辨别字段是否是主显示字段的Annotation
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Documented
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)
public @interface BAPIsMainDisplay {

}
