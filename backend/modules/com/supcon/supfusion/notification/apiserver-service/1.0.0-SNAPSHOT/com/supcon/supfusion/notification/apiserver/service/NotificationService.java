package com.supcon.supfusion.notification.apiserver.service;

import com.supcon.supfusion.notification.apiserver.service.bo.AckBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageBO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithMessageV1BO;
import com.supcon.supfusion.notification.apiserver.service.bo.SendWithTopicBO;

import java.util.List;

public interface NotificationService {
    /**
     * 兼容V1版本老接口
     *
     * @param sendWithMessageV1BO
     */
    String sendWithMessageV1(SendWithMessageV1BO sendWithMessageV1BO);

    /**
     * 消息内容直发接口
     *
     * @param sendWithMessageBO
     */
    String sendWithMessage(SendWithMessageBO sendWithMessageBO);

    /**
     * 根据主题发送消息接口
     *
     * @param sendWithMessageBO
     */
    String sendWithTopic(SendWithTopicBO sendWithMessageBO);

    /**
     * 消息状态
     *
     * @param ackBOS
     */
    void ack(List<AckBO> ackBOS);

//    /**
//     * 根据模板发送消息接口
//     *
//     * @param sendWithTmplateDTO
//     */
//    void sendWithTemplate(SendWithTmplateDTO sendWithTmplateDTO);
}
