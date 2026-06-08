package com.supcon.supfusion.notification.admin.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

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
public class NoticeTopic extends NoticeBase {
    //消息主题类型
    @TableField(value = "notice_topic_type_id")
    private Long type;

    @TableField(exist = false)
    private NoticeTopicTree topicTree;

    @TableField(exist = false)
    private String protocol_name;
    @TableField(exist = false)
    private String protocol_id;
    @TableField(exist = false)
    private String template_name;
    @TableField(exist = false)
    private String template_id;
    @TableField(value = "cover_sign")
    private Integer coverSign;


    //接收范围
    @TableField(exist = false)
    private List<Map<String, List<Object>>> receiveRange;
    //通知方式ID-消息模板ID
    @TableField(exist = false)
    private List<Long> tmpIdList;

    public static String getTopicTypeName() {
        return "notice_topic_type_id";
    }

    public static String getCoverSignFieldName() {
        return "cover_sign";
    }
}
