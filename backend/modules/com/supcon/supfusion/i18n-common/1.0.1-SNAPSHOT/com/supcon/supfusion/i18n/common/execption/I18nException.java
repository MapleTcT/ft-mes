package com.supcon.supfusion.i18n.common.execption;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 异常类
 */
public class I18nException extends BizException {
    public I18nException(ErrorDefinition definition) {
        super(definition);
    }

    public I18nException(ErrorDefinition definition, Object... args) {
        super(definition, args);
    }

    public I18nException(ErrorDefinition definition, Throwable throwable) {
        super(definition, throwable);
    }

    public I18nException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, throwable, args);
    }
}
