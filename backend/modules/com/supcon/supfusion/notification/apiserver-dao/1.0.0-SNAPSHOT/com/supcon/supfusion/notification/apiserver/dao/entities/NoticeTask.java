package com.supcon.supfusion.notification.apiserver.dao.entities;

import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 发送任务表
 * </p>
 *
 * @author panzk
 * @since 2020-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 发送任务表ID
     */
    private Long id;

    /**
     * 发送任务表CODE = id + date
     */
    private String code;

    /**
     * 业务方事务编号
     */
    private String bsmodCode;

    /**
     * 业务模块名称
     */
    private String bsmodName;

    /**
     * 任务类型,0topic 1message
     */
    private Integer taskType;

    /**
     * 任务状态
     */
    private Integer status;

    /**
     * 分表时间戳
     */
    private Long shardingTime;

    /**
     * 协议主题表ID
     */
    private Long noticeTopicId;

    public static String getTableName() {
        return "notice_task";
    }

    public static String getIdFieldName() {
        return "id";
    }

    public static String getCodeFieldName() {
        return "code";
    }

    public static String getBsmodCodeFieldName() {
        return "bsmod_code";
    }

    public static String getBsmodNameFieldName() {
        return "bsmod_name";
    }

    public static String getTaskTypeFieldName() {
        return "task_type";
    }

    public static String getStatusFieldName() {
        return "status";
    }

    public static String getShardingTimeFieldName() {
        return "sharding_time";
    }

    public static String getNoticeTopicIdFieldName() {
        return "notice_topic_id";
    }


}
