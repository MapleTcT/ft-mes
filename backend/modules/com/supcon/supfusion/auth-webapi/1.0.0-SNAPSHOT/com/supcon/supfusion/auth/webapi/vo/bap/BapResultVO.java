package com.supcon.supfusion.auth.webapi.vo.bap;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapResultVO<T> {
    private Integer code;
    private Boolean success;
    private T data;
    private String msg;
}
