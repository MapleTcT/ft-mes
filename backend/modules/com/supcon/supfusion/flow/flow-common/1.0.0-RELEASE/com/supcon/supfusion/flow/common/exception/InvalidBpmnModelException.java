/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年5月21日 上午11:27:40
 */
public class InvalidBpmnModelException extends BizException {

    private static final long serialVersionUID = -3944876096051623513L;

    /**
     * @param definition
     */
    public InvalidBpmnModelException(ErrorDefinition definition) {
        super(definition);
    }
    
    public InvalidBpmnModelException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
