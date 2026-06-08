/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年5月20日 上午9:27:40
 */
public class UniqueConstraintsException extends BizException {

    private static final long serialVersionUID = -585121736125192820L;

    /**
     * @param definition
     */
    public UniqueConstraintsException(ErrorDefinition definition) {
        super(definition);
    }
    
    public UniqueConstraintsException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
