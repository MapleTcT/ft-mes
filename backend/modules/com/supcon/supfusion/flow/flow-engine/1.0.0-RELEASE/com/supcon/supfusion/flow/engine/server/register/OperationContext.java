/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.register;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;
import com.supcon.supfusion.flow.engine.server.service.OperationApi;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年6月13日 下午4:27:30
 */
@Slf4j
public class OperationContext {
    
    private Map<Object, Class<?>> instanceMap;
    private ApplicationContext springContext;
    
    public OperationContext(Map<Object, Class<?>> instanceMap, ApplicationContext springContext) {
        this.instanceMap = instanceMap;
        this.springContext = springContext;
    }
    
    public OperationApi getInstance(OperationTypeEnum operationEnum) {
        if (operationEnum == null) {
            return null;
        }
        Class<?> clazz = instanceMap.get(operationEnum);
        if (clazz == null) {
            log.error("当前操作类型不存在, 请求类型: {}", operationEnum);
            return null;
        }
        return (OperationApi)springContext.getBean(clazz);
    }
}
