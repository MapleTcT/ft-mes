package com.supcon.supfusion.notification.apiserver.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName(value = "notice_topic_tmpl_rel",autoResultMap = true)
public class NoticeTopicTmplateRelation extends BaseEntity {
    @TableField(value = "id")
    private Long id;
    //消息主题
    @TableField(value = "notice_topic_id")
    private Long topic;
    //消息模板
    @TableField(value = "notice_tmpl_id")
    private Long template;
    ///通知方式
    @TableField(value = "notice_protocol_id")
    private Long protocol;

    public static String getTopicIdName() {
        return "notice_topic_id";
    }

    public static String getTemplateIdName() {
        return "notice_tmpl_id";
    }

    public static String getProtocolIdName() {
        return "notice_protocol_id";
    }

    public static String getIdName() {
        return "id";
    }


}
