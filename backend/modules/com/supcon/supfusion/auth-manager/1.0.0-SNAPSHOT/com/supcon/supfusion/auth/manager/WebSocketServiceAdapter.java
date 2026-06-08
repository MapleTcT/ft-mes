package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.ws.client.dto.NoticeMessageDTO;

import java.util.List;

/**
 * @author caokele
 */
public interface WebSocketServiceAdapter {

    void pushNoticeMessages(String topic, List<NoticeMessageDTO> messages);
}
