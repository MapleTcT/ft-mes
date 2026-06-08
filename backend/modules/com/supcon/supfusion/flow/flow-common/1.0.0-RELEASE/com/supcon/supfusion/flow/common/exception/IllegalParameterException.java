/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.Biz400Exception;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年9月12日 上午9:32:19
 */
public class IllegalParameterException extends Biz400Exception {
    
    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public IllegalParameterException(ErrorDefinition definition) {
        super(definition);
    }
    
    public IllegalParameterException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
