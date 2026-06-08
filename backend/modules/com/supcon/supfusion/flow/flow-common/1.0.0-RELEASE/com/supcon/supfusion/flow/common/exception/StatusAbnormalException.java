/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年5月20日 下午18:27:40
 */
public class StatusAbnormalException extends BizException {

    private static final long serialVersionUID = -5354119590047513513L;

    /**
     * 状态异常类 -- 流程状态异常, 待办状态异常 eg.
     * @param definition
     */
    public StatusAbnormalException(ErrorDefinition definition) {
        super(definition);
    }
    
    public StatusAbnormalException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
    
}
