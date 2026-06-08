package com.supcon.supfusion.notification.apiserver.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.Data;

@Data
public class MessageBO extends BO {
    /**
     * 协议类型
     */
    private String protocol;
    /**
     * 消息内容
     */
    private String content;
}
