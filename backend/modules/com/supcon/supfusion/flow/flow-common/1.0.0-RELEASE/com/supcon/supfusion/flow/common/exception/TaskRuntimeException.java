/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午10:49:24
 */
public class TaskRuntimeException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public TaskRuntimeException(ErrorDefinition definition) {
        super(definition);
    }
    
    public TaskRuntimeException(ErrorDefinition definition, Object... params) {
        super(definition, params);
    }
    
    public TaskRuntimeException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
    
    public TaskRuntimeException(ErrorDefinition definition, Throwable throwable, Object... params) {
        super(definition, throwable, params);
    }

}
