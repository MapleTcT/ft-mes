package com.supcon.supfusion.notification.app.manager.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.notification.app.manager.WebSocketService;
import com.supcon.supfusion.notification.app.manager.fegin.dto.WebSocketMessageDTO;
import com.supcon.supfusion.notification.app.manager.fegin.dto.WebSocketResponseDTO;
import com.supcon.supfusion.ws.client.NoticeApiClient;
import com.supcon.supfusion.ws.client.dto.NoticeMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("supplantWebSocket")
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    @ServiceApiReference
    private NoticeApiClient noticeApiClient;

    @Override
    public WebSocketResponseDTO pushMessage(String topic, List<WebSocketMessageDTO> webSocketMessageDTOS) {
        if (webSocketMessageDTOS == null) {
            return null;
        }

        String requestString = JSONArray.toJSONString(webSocketMessageDTOS);
        log.info("request websocket with {}", requestString);
        List<NoticeMessageDTO> noticeMessageDTOS = JSONArray.parseArray(requestString).toJavaList(NoticeMessageDTO.class);

        com.supcon.supfusion.ws.client.dto.WebSocketResponseDTO responseDTO = noticeApiClient.pushMessages(topic, RpcContext.getContext().getTenantId(), noticeMessageDTOS);

        String responseString = JSONObject.toJSONString(responseDTO);
        log.info("websocket return response {}", responseString);
        WebSocketResponseDTO webSocketResponseDTO = JSONObject.parseObject(responseString, WebSocketResponseDTO.class);

        return webSocketResponseDTO;
    }
}
