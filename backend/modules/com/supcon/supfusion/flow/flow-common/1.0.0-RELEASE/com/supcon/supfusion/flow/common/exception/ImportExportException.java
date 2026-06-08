/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年11月09日 上午10:27:40
 */
public class ImportExportException extends BizException {

    private static final long serialVersionUID = 1L;

    /**
     * @param definition
     */
    public ImportExportException(ErrorDefinition definition) {
        super(definition);
    }
    
    public ImportExportException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
