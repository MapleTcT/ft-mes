package com.supcon.supfusion.auth.webapi.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;
import lombok.Data;

@JsonPropertyOrder({"code", "message", "userSessionInfo"})
@Data
public class UserSessinResult<T> extends ErrorEntity {

    private static final long serialVersionUID = 2368049747710991182L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T userSessionInfo;

    public UserSessinResult() {
    }

    public UserSessinResult(int httpCode, int code, String message) {
        super(code, message);
    }

    public UserSessinResult(int httpCode, int code, String message, T userSessionInfo) {
        super(code, message);
        this.userSessionInfo = userSessionInfo;
    }

    public UserSessinResult(T data) {
        this();
        this.userSessionInfo = data;
    }

}
