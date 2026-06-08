package com.supcon.supfusion.notification.sms.dao.entities;

import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 未读消息数量统计表
 * </p>
 *
 * @author panzk
 * @since 2020-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeMessageUnreadCount extends BaseEntity {

    private static final long serialVersionUID = 1L;

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
     * 主题
     *
     * @return
     */
    private Long topicId;


    public static String getIdFieldName() {
        return "id";
    }

    public static String getStaffCodeFieldName() {
        return "staff_code";
    }

    public static String getNoticeProtocolIdFieldName() {
        return "notice_protocol_id";
    }

    public static String getUnreadCountFieldName() {
        return "unread_count";
    }

    public static String getTopicIdFieldName() {
        return "topic_id";
    }

}
