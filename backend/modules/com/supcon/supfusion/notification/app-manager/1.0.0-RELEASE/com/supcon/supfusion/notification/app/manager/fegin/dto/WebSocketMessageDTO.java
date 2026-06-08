package com.supcon.supfusion.notification.app.manager.fegin.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

@Data
public class WebSocketMessageDTO extends DTO {
    private String userName;
    private NoticeADP data;
}
