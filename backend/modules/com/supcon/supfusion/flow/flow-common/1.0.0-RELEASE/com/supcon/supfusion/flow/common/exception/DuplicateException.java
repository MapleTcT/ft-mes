/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年08月25日 上午9:27:40
 */
public class DuplicateException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public DuplicateException(ErrorDefinition definition) {
        super(definition);
    }
    
    public DuplicateException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
