package com.supcon.supfusion.notification.apiserver.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import com.supcon.supfusion.notification.protocol.common.ReadStatus;
import com.supcon.supfusion.notification.protocol.common.SendStatus;
import lombok.Data;

import java.util.List;

@Data
public class AckBO extends BO {
    /**
     * 消息协议类型
     */
    private String protocol;
    /**
     * 消息发送时间
     */
    private String time;
    /**
     * 消息唯一ID
     */
    private String messageId;
    /**
     * 消息发送状态
     */
    private SendStatus sendStatus;
    /**
     * 消息阅读状态
     */
    private ReadStatus readStatus;
    /**
     * 失败原因,sendStatus为FAIL时生效
     */
    private String errorMessage;
}
