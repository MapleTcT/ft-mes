/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import org.springframework.aop.framework.AopContext;

/**
 * @author: zhuangmh
 * @date: 2020年8月3日 下午3:02:23
 */
public class ProxyUtils {
    
    private ProxyUtils() {
        throw new IllegalStateException("ProxyUtils is utility class, do not instantiate");
    }
    
    /**
     * 获取代理对象
     * @param proxiedClass 被代理类
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxyObject(Class<T> proxiedClass) {
        Object currentProxy = AopContext.currentProxy();
        return (T)currentProxy;
    }
}
