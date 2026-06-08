/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年6月15日 上午9:27:40
 */
public class FormatException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public FormatException(ErrorDefinition definition) {
        super(definition);
    }
    
    public FormatException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
    
}
