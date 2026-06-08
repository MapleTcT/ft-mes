package com.supcon.supfusion.notification.admin.webapi.vo;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeMsg;
import lombok.Data;

@Data
public class UnreadCountMessageVO {
    /**
     * 未读消息数量统计表ID
     */
    private Long id;

    /**
     * 人员code
     */
    private String staffCode;

    /**
     * 协议表ID
     */
    private Long noticeProtocolId;

    /**
     * 未读消息数量统计
     */
    private Long unreadCount;

    /**
     * 主题id
     * @return
     */
    private Long topicId;
    /**
     * 主题名字
     * @return
     */
    private String topicName;
    /**
     * 修改时间
     * @return
     */
    private Long modifyTime;

    private NoticeProtocolMessageVO noticeMsg;
}
