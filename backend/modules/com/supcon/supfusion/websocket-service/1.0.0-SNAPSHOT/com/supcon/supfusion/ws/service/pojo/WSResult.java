package com.supcon.supfusion.ws.service.pojo;

import lombok.Data;

@Data
public class WSResult {
    private Integer code;
    private String message;
    private Object data;
}
