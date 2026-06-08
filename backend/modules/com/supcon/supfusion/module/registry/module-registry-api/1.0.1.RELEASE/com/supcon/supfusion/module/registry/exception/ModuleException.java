/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.module.registry.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年7月28日 上午11:15:21
 */
public class ModuleException extends BizException {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public ModuleException(ErrorDefinition definition) {
        super(definition);
    }
    
    public ModuleException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
