package com.supcon.supfusion.notification.admin.dao.entities;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 14:48
 */
@Getter
@Setter
@ToString
@TableName(value = "notice_topic", autoResultMap = true)
public class NoticeTopicList extends BaseEntity {
    private Long topicId;
    private String topicName;
    private String topicCode;
    private String templateIds;
    private String rangeIds;
}
