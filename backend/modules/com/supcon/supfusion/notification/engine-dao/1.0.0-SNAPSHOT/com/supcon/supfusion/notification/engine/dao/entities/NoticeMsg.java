package com.supcon.supfusion.notification.engine.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 消息表
 * </p>
 *
 * @author panzk
 * @since 2020-05-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeMsg extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 消息表ID
     */
    private Long id;

    /**
     * 人员ID
     */
    private String staffCode;

    /**
     * 人员名
     */
    private String staffName;
    /**
     * 用户名　兼容通知中心1.0的发送接口
     */
    private String userName;
    /**
     * 用户名　'业务方事务编号,用于站内信查询'
     */
    private String bsmodCode;
    /**
     * 用户名　'业务模块名称,用于站内信查询'
     */
    private String bsmodName;
    /**
     * 用户名　'主题名称,用于站内信查询'
     */
    private String topicName;

    /**
     * 发送状态 0失败，1成功，2 未知，3待查询(只有状态3才会触发回执查询)，9已分发
     */
    private Integer sendStatus;

    /**
     * 发送失败返回结果
     */
    private String errorResult;

    /**
     * 阅读状态 0未读，1已读，2未知(只有状态0才会触发回执查询)
     */
    private Integer readStatus;

    /**
     * 失败重试次数
     */
    private Integer retry;

    /**
     * 分表时间戳
     */
    private Long shardingTime;

    /**
     * 发送任务表ID
     */
    private Long noticeTaskId;

    /**
     * 协议表ID
     */
    private Long noticeProtocolId;

    /**
     * 发送任务协议表ID
     */
    private Long noticeTaskProtocolId;

    /**
     * 消息内容入参
     */
    private String param;
    /**
     * 消息主题ID
     */
    private Long topicId;


    public static String getTableName() {
        return "notice_msg";
    }

    public static String getIdFieldName() {
        return "id";
    }

    public static String getStaffCodeFieldName() {
        return "staff_code";
    }

    public static String getStaffNameFieldName() {
        return "staff_name";
    }

    public static String getSendStatusFieldName() {
        return "send_status";
    }

    public static String getResultFieldName() {
        return "error_result";
    }

    public static String getReadStatusFieldName() {
        return "read_status";
    }

    public static String getRetryFieldName() {
        return "retry";
    }

    public static String getShardingTimeFieldName() {
        return "sharding_time";
    }

    public static String getNoticeTaskIdFieldName() {
        return "notice_task_id";
    }

    public static String getNoticeProtocolIdFieldName() {
        return "notice_protocol_id";
    }

    public static String getNoticeTaskProtocolIdFieldName() {
        return "notice_task_protocol_id";
    }

    public static String getUserNameFieldName() {
        return "user_name";
    }

    public static String getBsmodCodeFieldName() {
        return "bsmod_code";
    }

    public static String getBsmodNameFieldName() {
        return "bsmod_name";
    }

    public static String getTopicNameFieldName() {
        return "topic_name";
    }

    public static String getParamFieldName() {
        return "param";
    }

}
