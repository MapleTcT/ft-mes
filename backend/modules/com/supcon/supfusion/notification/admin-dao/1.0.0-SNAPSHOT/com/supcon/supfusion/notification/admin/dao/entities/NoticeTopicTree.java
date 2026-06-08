package com.supcon.supfusion.notification.admin.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@ToString(exclude="topicTreeChlid")
@TableName(value = "notice_topic_type",autoResultMap = true)
public class NoticeTopicTree extends NoticeBase {
    //父级消息主题类型
    @TableField(value = "parent_id")
    private Long parentId;

    @TableField(exist=false)
    private NoticeTopicTree parentObj;

    //子集消息主题类型
//    @TableField(exist=false)
    //private Set<Long> topicTreeChlid;
    //层级结构  todo
    @TableField(value = "lay_rec")
    private Integer layRec;
    
    private String i18nKey;

    public static String getParentIdName() {
        return "parent_id";
    }

}
