package com.supcon.supfusion.signature.dao.annotation;

import java.lang.annotation.*;

/**
 * @author wuqi 用于标识包含自定义字段的类，以及标识系统编码/对象类型的自定义字段
 */
@Documented
@Target(value = { ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface BAPCustomComponent {
	/**
	 * property code
	 */
	String code() default "";
	
	/**
	 * 标识是否是系统编码或对象类型
	 */
	boolean complex() default false;
}
