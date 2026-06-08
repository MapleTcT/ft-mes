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
public class ProcessOperateException extends BizException {

    private static final long serialVersionUID = 8426618853700019014L;

    /**
     * @param definition
     */
    public ProcessOperateException(ErrorDefinition definition) {
        super(definition);
    }
    
    public ProcessOperateException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
    
}
