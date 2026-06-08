package com.supcon.supfusion.auth.webapi.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.exception.ErrorEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"code", "message", "userInfo"})
@Data
public class UserInfoResult<T> extends ErrorEntity {

    private static final long serialVersionUID = 2368049747710991182L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T userInfo;

    public UserInfoResult() {
    }

    public UserInfoResult(int httpCode, int code, String message) {
        super(code, message);
    }

    public UserInfoResult(int httpCode, int code, String message, T userInfo) {
        super(code, message);
        this.userInfo = userInfo;
    }

    public UserInfoResult(T data) {
        this();
        this.userInfo = data;
    }

}
