package com.supcon.supfusion.notification.mobile.service.impl;

import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.notification.mobile.manager.WebSocketService;
import com.supcon.supfusion.notification.mobile.manager.fegin.dto.*;
import com.supcon.supfusion.notification.mobile.service.MobileService;
import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.AckResult;
import com.supcon.supfusion.notification.protocol.model.Notice;
import com.supcon.supfusion.notification.protocol.model.Receiver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.util.*;


/**
 * @author zyf
 * @daTe 2020/12/31 9:41
 */
@Service
@Slf4j
public class MobileServiceImpl implements MobileService {

    @Qualifier("mobileWebSocket")
    @Autowired
    private WebSocketService webSocketService;

    @Override
    public Ack send(Notice notice) {
        try {
            if (!StringUtils.isEmpty(notice.getContent()) && notice.getReceivers() != null) {
                List<WebSocketMessageDTO> webSocketMessageDTOS = new ArrayList();
                List<AckResult> ackResults = new ArrayList<>();
                //遍历接收者批量发送
                for (Receiver receiver : notice.getReceivers()) {
                    WebSocketMessageDTO webSocketMessageDTO = new WebSocketMessageDTO();
                    NoticeMobile mobile = new NoticeMobile();
                    mobile.setId(receiver.getMessageId());
                    mobile.setSender(notice.getBsmodName());
                    mobile.setContent(notice.getContent());
                    mobile.setTopic(notice.getTopic());
                    mobile.setShardingTime(notice.getTime());
                    mobile.setParam(notice.getParam());
                    webSocketMessageDTO.setUserName(receiver.getAddress());
                    webSocketMessageDTO.setData(mobile);
                    webSocketMessageDTOS.add(webSocketMessageDTO);
                }
                WebSocketResponseDTO webSocketResponseDTO;
                try {
                    webSocketResponseDTO = webSocketService.pushMessage(webSocketMessageDTOS);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return Ack.buildFail(notice, "websocket call failed", ReadStatus.UNREAD, SendStatus.FAIL);
                }

                log.info("websocket return: {}", webSocketResponseDTO.toString());
                if (BizErrorEnum.SYSTEM_OK.getCode().equals(webSocketResponseDTO.getCode())) {
                    /**
                     * 移动端全部发送成功
                     */
                    return Ack.buildSuccess(notice, ReadStatus.UNREAD);
                } else {
                    ResponseData responseData = webSocketResponseDTO.getData();
                    if (responseData == null) {
                        /**
                         * websocket返回内容不全, 全部作为失败
                         */
                        log.error("websocket返回内容不全: {}", webSocketResponseDTO.toString());
                        return Ack.buildFail(notice, "send mobile failed", ReadStatus.UNREAD, SendStatus.FAIL);
                    }
                    List<Fail> fails = responseData.getFail();
                    if (fails == null || fails.size() == 0) {
                        /**
                         * websocket返回内容不全, 全部作为失败
                         */
                        log.error("websocket返回内容不全: {}", webSocketResponseDTO.toString());
                        return Ack.buildFail(notice, "send mobile failed", ReadStatus.UNREAD, SendStatus.FAIL);
                    }

                    /**
                     * 移动端部分发送成功
                     */
                    Map<String, String> userIdFailMessage = new HashMap<>();
                    for (Fail fail : fails) {
                        userIdFailMessage.put(fail.getUserName(), fail.getMsg());
                    }

                    notice.getReceivers().forEach(receiver -> {
                        String userId = receiver.getAddress();
                        if (StringUtils.isEmpty(userId)) {
                            /**
                             *  没有绑定用户的人员，在websocket发送前就已经作失败处理
                             */
                            return;
                        }

                        AckResult ackResult = new AckResult();
                        if (userIdFailMessage.containsKey(userId)) {
                            ackResult.setMessageId(receiver.getMessageId());
                            ackResult.setReadStatus(ReadStatus.UNREAD);
                            ackResult.setSendStatus(SendStatus.FAIL);
                            ackResult.setErrorMessage(userIdFailMessage.get(userId));
                        } else {
                            ackResult.setMessageId(receiver.getMessageId());
                            ackResult.setReadStatus(ReadStatus.UNREAD);
                            ackResult.setSendStatus(SendStatus.SUCCESS);
                        }
                        ackResults.add(ackResult);
                    });
                    return Ack.builder().results(ackResults).build();
                }
            } else {
                log.error("移动端发送内容不全，{}", notice);
                return Ack.buildFail(notice, "incomplete message sent in the mobile", ReadStatus.UNREAD, SendStatus.FAIL);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Ack.buildFail(notice, "send mobile failed", ReadStatus.UNREAD, SendStatus.FAIL);
        }
    }

}
