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
@TableName(value = "notice_topic_range", autoResultMap = true)
public class NoticeRecieveRange extends BaseEntity {
    @TableField(value = "id")
    private Long id;
    //推送方式, 分为0人员、1岗位、2部门、3角色、4业务规则
    @TableField(value = "range_type")
    private Integer rangeType;
    //业务模块名称
    @TableField(value = "bsmod_name")
    private String bizModuleName;
    //业务模块编码
    @TableField(value = "bsmod_code")
    private String bizModuleCode;
    //业务模块提供接口地址，返回人员字符串
    @TableField(value = "bsmod_addr")
    private String bizModuleAddr;
    //通知方式ID
    @TableField(value = "notice_topic_id")
    private String noticeTopicId;

    public static String getTopicIdFieldName() {
        return "notice_topic_id";
    }

    public static String getRangeTypeFieldName() {
        return "range_type";
    }

    public static String getIdFieldName() {
        return "id";
    }
}
