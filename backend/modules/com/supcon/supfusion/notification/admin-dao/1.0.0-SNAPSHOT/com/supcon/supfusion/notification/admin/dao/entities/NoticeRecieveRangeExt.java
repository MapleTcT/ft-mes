package com.supcon.supfusion.notification.admin.dao.entities;

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
 * @create 2020/6/11 13:03
 */
@Getter
@Setter
@ToString
@TableName(value = "notice_topic_range_ext",autoResultMap = true)
public class NoticeRecieveRangeExt extends BaseEntity {
    @TableField(value = "id")
    private Long id;
    @TableField(value = "receiver_id")
    private Long receiverId;
    //'当推送方式为人员、部门、岗位时对应的CODE',
    @TableField(value = "receiver_code")
    private String receiverCode;
    //'0 不包含子级, 1 包含子级' 范围为部门岗位时是否包含下级
    @TableField(value = "contain_children")
    private Integer containChildren;
    //主题发送范围表ID
    @TableField(value = "notice_topic_range_id")
    private Long noticeTopicRangeId;

    public static String getRangeIdFieldName() {
        return "notice_topic_range_id";
    }
}
