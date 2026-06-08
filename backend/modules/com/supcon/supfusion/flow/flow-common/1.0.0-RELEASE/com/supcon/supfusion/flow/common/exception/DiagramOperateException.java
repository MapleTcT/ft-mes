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
public class DiagramOperateException extends BizException {

    private static final long serialVersionUID = 800760174712137815L;

    /**
     * @param definition
     */
    public DiagramOperateException(ErrorDefinition definition) {
        super(definition);
    }
    
    public DiagramOperateException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
    
}
