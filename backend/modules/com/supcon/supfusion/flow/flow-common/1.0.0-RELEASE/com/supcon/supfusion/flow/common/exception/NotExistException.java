/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.Biz400Exception;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * @author: zhuangmh
 * @date: 2020年5月20日 上午9:27:40
 */
public class NotExistException extends Biz400Exception {

    private static final long serialVersionUID = 6274112331247567820L;

    /**
     * @param definition
     */
    public NotExistException(ErrorDefinition definition) {
        super(definition);
    }
    
    public NotExistException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }
}
