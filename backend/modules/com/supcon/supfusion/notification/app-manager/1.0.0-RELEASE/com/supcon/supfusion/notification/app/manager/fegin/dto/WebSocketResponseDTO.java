package com.supcon.supfusion.notification.app.manager.fegin.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class WebSocketResponseDTO extends DTO {
    private Integer code;
    private String message;
    private ResponseData data;
}
