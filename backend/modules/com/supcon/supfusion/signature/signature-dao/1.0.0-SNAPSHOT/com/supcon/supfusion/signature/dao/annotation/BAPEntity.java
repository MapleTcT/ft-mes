/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.signature.dao.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BAP辨别字段是否实体code
 * @author qy
 *
 */
@Documented
@Target({ElementType.TYPE}) 
@Retention(RUNTIME)
public @interface BAPEntity {
	

    /**
     * (Optional) The name of the entity.
     * <p> Defaults to the entity name.
     */
    String entityCode() default "";

}
