package com.supcon.supfusion.ws.service.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String method() default "";

    String uriPattern() default "";
}
