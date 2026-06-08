package com.supcon.supfusion.rbac.urlscan.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * @description: 菜单操作扫描注解
 * @author: 袁阳
 * @date: 2020/6/22
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MenuOperateCode {

    String[] value();
}
