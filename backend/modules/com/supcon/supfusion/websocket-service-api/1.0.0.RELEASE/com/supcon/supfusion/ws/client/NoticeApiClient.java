package com.supcon.supfusion.ws.client;

import com.supcon.supfusion.ws.client.dto.NoticeMessageDTO;
import com.supcon.supfusion.ws.client.dto.WebSocketResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author lifangyuan
 */
@FeignClient("ws")
public interface NoticeApiClient {

    /**
     * 给指定主题下指定用户发送通知
     * @param topic 消息主题
     * @param tenantId 租户id
     * @param messages 消息列表
     */
    @PostMapping("/service-api/ws/v1/notice/topic-user/{topic}")
    @ResponseBody
    WebSocketResponseDTO pushMessages(@PathVariable("topic") String topic, @RequestHeader("X-Tenant-Id") String tenantId, @RequestBody List<NoticeMessageDTO> messages);

    /**
     * 给指定主题下所有用户发送通知
     * WebSocketResponseDTO topic 消息主题
     * @param message 消息
     */
    @PostMapping("/service-api/ws/v1/notice/topic/{topic}")
    @ResponseBody
    WebSocketResponseDTO pushTopicMessages(@PathVariable("topic") String topic, @RequestHeader("X-Tenant-Id") String tenantId, @RequestBody Object message);

}
