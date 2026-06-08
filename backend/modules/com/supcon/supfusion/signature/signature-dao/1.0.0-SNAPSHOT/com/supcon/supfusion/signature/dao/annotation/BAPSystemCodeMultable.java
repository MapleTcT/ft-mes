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
 * 判断字段是否是系统编码多选
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Documented
@Target({ FIELD, METHOD })
@Retention(RUNTIME)
public @interface BAPSystemCodeMultable {

}
