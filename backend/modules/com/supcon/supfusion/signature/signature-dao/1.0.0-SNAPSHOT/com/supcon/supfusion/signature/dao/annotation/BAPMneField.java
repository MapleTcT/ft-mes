/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Target(ElementType.TYPE) 
@Retention(RUNTIME)
public @interface BAPMneField {
	String name() default "";
}
