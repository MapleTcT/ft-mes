package com.supcon.supfusion.notification.app.manager;

import com.supcon.supfusion.notification.app.manager.fegin.dto.WebSocketMessageDTO;
import com.supcon.supfusion.notification.app.manager.fegin.dto.WebSocketResponseDTO;

import java.util.List;

public interface WebSocketService {
    WebSocketResponseDTO pushMessage(String topic, List<WebSocketMessageDTO> webSocketMessageDTOS);
}
