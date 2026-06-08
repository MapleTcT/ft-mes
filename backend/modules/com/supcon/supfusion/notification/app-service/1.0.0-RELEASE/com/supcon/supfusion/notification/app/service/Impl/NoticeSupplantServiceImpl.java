package com.supcon.supfusion.notification.app.service.Impl;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.framework.cloud.common.exception.BizErrorEnum;
import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import com.supcon.supfusion.notification.protocol.model.Ack;
import com.supcon.supfusion.notification.protocol.model.AckResult;
import com.supcon.supfusion.notification.protocol.model.Notice;
import com.supcon.supfusion.notification.protocol.model.Receiver;
import com.supcon.supfusion.notification.app.manager.WebSocketService;
import com.supcon.supfusion.notification.app.manager.fegin.dto.Fail;
import com.supcon.supfusion.notification.app.manager.fegin.dto.NoticeADP;
import com.supcon.supfusion.notification.app.manager.fegin.dto.ResponseData;
import com.supcon.supfusion.notification.app.manager.fegin.dto.WebSocketMessageDTO;
import com.supcon.supfusion.notification.app.manager.fegin.dto.WebSocketResponseDTO;
import com.supcon.supfusion.notification.app.service.NoticeSupplantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/21 14:38
 */
@Service("adp")
@Slf4j
public class NoticeSupplantServiceImpl implements NoticeSupplantService {
    @Resource(name = "supplantWebSocket")
    private WebSocketService webSocketService;

    @Override
    public Ack send(Notice notice) {
        try {
            if (!StringUtils.isEmpty(notice.getContent()) && notice.getReceivers() != null) {
                List<WebSocketMessageDTO> webSocketMessageDTOS = new ArrayList();
                List<AckResult> ackResults = new ArrayList<>();
                //遍历接收者批量发送
                for (Receiver receiver : notice.getReceivers()) {
                    String address = receiver.getAddress();
                    if (StringUtils.isEmpty(address)) {
                        AckResult ackResult = new AckResult();
                        ackResult.setMessageId(receiver.getMessageId());
                        ackResult.setReadStatus(ReadStatus.UNREAD);
                        ackResult.setSendStatus(SendStatus.FAIL);
                        ackResult.setErrorMessage("该人员未绑定用户");
                        ackResults.add(ackResult);
                        continue;
                    }
                    WebSocketMessageDTO webSocketMessageDTO = new WebSocketMessageDTO();
                    NoticeADP noticeADP = new NoticeADP();
                    noticeADP.setId(receiver.getMessageId());
                    noticeADP.setSender(notice.getBsmodName());
                    noticeADP.setContent(notice.getContent());
                    noticeADP.setTopic(notice.getTopic());
                    noticeADP.setShardingTime(notice.getTime());
                    noticeADP.setParam(notice.getParam());
                    webSocketMessageDTO.setUserName(address);
                    webSocketMessageDTO.setData(noticeADP);
                    webSocketMessageDTOS.add(webSocketMessageDTO);
                }
                WebSocketResponseDTO webSocketResponseDTO;
                try {
                    webSocketResponseDTO = webSocketService.pushMessage("app", webSocketMessageDTOS);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return Ack.buildFail(notice, "websocket call failed", ReadStatus.UNREAD, SendStatus.FAIL);
                }

                log.info("websocket return: {}", webSocketResponseDTO.toString());
                if (BizErrorEnum.SYSTEM_OK.getCode().equals(webSocketResponseDTO.getCode()) && ackResults.size() <= 0) {
                    /**
                     * 业务通知全部发送成功
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
                     * 业务通知部分发送成功
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
                log.error("业务通知发送内容不全，{}", notice);
                return Ack.buildFail(notice, "incomplete message sent in the mobile", ReadStatus.UNREAD, SendStatus.FAIL);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Ack.buildFail(notice, "send mobile failed", ReadStatus.UNREAD, SendStatus.FAIL);
        }
    }

}
