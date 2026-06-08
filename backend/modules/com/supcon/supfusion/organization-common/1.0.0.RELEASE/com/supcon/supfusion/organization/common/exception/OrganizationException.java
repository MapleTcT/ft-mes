package com.supcon.supfusion.organization.common.exception;

import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorDefinition;

/**
 * 组织架构异常类
 */
public class OrganizationException extends BizHttpStatusException {
    public OrganizationException(ErrorDefinition definition) {
        super(definition, 400);
    }

    public OrganizationException(ErrorDefinition definition, Object... args) {
        super(definition, 400, args);
    }

    public OrganizationException(ErrorDefinition definition, Throwable throwable) {
        super(definition, 400, throwable);
    }

    public OrganizationException(ErrorDefinition definition, Throwable throwable, Object... args) {
        super(definition, 400, throwable, args);
    }

    public OrganizationException(Integer code, String message) {
        super(new ErrorDefinition() {
            @Override
            public Integer getCode() {
                return code;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public String getInfo() {
                return message;
            }
        }, 400);
    }

    public OrganizationException(Integer code, String message, Integer status) {
        super(new ErrorDefinition() {
            @Override
            public Integer getCode() {
                return code;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public String getInfo() {
                return message;
            }
        }, status);
    }
}
