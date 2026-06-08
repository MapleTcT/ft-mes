package com.supcon.supfusion.auth.webapi.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.supcon.supfusion.framework.cloud.common.pojo.POJO;
import lombok.Data;

/**
 * @author lifangyuan
 */
@JsonPropertyOrder({"code", "message", "result"})
@Data
public class SuposResult<T> implements POJO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T userInfo;

    public SuposResult() {

    }

    public SuposResult(T data) {
        this.userInfo = data;
    }

}
