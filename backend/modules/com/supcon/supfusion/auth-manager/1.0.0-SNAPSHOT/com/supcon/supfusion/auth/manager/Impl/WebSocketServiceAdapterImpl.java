package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.manager.WebSocketServiceAdapter;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.ws.client.NoticeApiClient;
import com.supcon.supfusion.ws.client.dto.NoticeMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author caokele
 */
@Slf4j
@Service
public class WebSocketServiceAdapterImpl implements WebSocketServiceAdapter {
    @Autowired
    private NoticeApiClient noticeApiClient;

    @Override
    public void pushNoticeMessages(String topic, List<NoticeMessageDTO> messages) {
        String tenantId = RpcContext.getContext().getTenantId();
        noticeApiClient.pushMessages(topic, tenantId, messages);
    }
}
