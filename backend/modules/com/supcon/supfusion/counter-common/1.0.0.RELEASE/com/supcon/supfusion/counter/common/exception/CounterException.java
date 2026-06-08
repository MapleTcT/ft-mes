package com.supcon.supfusion.counter.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterException extends BizException {

    public CounterException(ErrorDefinition definition) {
        super(definition);
    }

    public CounterException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public CounterException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }

    public CounterException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }
}
