package com.supcon.supfusion.file.server.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

public class FileServerException extends BizException{
    public FileServerException(ErrorDefinition definition) {
        super(definition);
    }
}