package com.supcon.supfusion.notification.mobile.manager;


import com.supcon.supfusion.notification.mobile.manager.fegin.dto.WebSocketMessageDTO;
import com.supcon.supfusion.notification.mobile.manager.fegin.dto.WebSocketResponseDTO;

import java.util.List;

public interface WebSocketService {
    WebSocketResponseDTO pushMessage(List<WebSocketMessageDTO> webSocketMessageDTOS);
}
