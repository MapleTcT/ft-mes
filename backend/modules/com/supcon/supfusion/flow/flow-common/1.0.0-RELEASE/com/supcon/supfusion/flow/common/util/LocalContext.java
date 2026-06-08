/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.util;

import java.util.List;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.supcon.supfusion.flow.common.dto.AssigneeDTO;
import com.supcon.supfusion.flow.common.enumeration.OperationTypeEnum;

import lombok.Getter;
import lombok.Setter;

/**
 * @author: zhuangmh
 * @date: 2020年8月17日 下午3:21:50
 */
public final class LocalContext {
    
    private LocalContext() {
        
    }
    
    private static final ThreadLocal<LocalContext> LOCAL_CONTEXT_THREAD_LOCAL = new TransmittableThreadLocal<LocalContext>() {
        @Override
        protected LocalContext initialValue() {
            return new LocalContext();
        }
    };
    
    public static LocalContext getContext() {
        return LOCAL_CONTEXT_THREAD_LOCAL.get();
    }
    
    /**
     * 移除上下文
     */
    public static void removeContext() {
        LOCAL_CONTEXT_THREAD_LOCAL.remove();
    }
    /**
     * 当前流程实例ID
     */
    @Getter
    @Setter
    private String processId;
    /**
     * 当前提交者
     */
    @Getter
    @Setter
    private Long submitter;
    /**
     * 操作类型 
     */
    @Getter
    @Setter
    private OperationTypeEnum operationType;
    /**
     * 将请求参数在上下文传递, 以便在监听器中获取
     */
    @Getter
    @Setter
    private List<AssigneeDTO> assigns;
}
