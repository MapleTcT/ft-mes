package com.supcon.supfusion.notification.admin.dao.entities;

import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
    * 发送任务协议表
    * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeTaskProtocol extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 发送任务协议表ID
     */
    private Long id;

    /**
     * 协议表ID
     */
    private Long noticeProtocolId;

    /**
     * 发送任务表ID
     */
    private Long noticeTaskId;

    /**
     * 消息内容
     */
    private String content;


    public static String getIdFieldName() {
        return "id";
    }
    public static String getNoticeProtocolIdFieldName() {
        return "notice_protocol_id";
    }
    public static String getNoticeTaskIdFieldName() {
        return "notice_task_id";
    }
    public static String getContentFieldName() {
        return "template";
    }

}
