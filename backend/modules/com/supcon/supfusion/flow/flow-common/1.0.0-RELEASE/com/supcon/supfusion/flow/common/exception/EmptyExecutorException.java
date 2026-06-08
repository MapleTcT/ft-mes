/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.Biz400Exception;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年6月3日 上午10:49:24
 */
public class EmptyExecutorException extends Biz400Exception {

    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public EmptyExecutorException(ErrorDefinition definition) {
        super(definition);
    }
    
    public EmptyExecutorException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }
    
    public EmptyExecutorException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

}
