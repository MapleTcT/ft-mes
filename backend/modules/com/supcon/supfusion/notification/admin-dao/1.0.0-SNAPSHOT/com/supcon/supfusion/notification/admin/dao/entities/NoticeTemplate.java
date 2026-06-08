package com.supcon.supfusion.notification.admin.dao.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@TableName(value = "notice_tmpl", autoResultMap = true)
@ApiModel(value = "消息模板对象", description = "用户信息")
public class NoticeTemplate extends NoticeBase {

    @ApiModelProperty(value = "通知方式", name = "noticeType")
    @TableField(value = "notice_protocol_id")
    private Long noticeType;

    @TableField(exist = false)
    private NoticeProtocol noticeProtocol;

    @ApiModelProperty(value = "模板内容", name = "template", example = "这是消息模板${filed}")
    @TableField(value = "template")
    private String template;

    @ApiModelProperty(value = "模板默认参数", name = "params", example = "{\"filed\":\"value\"}")
    @TableField(value = "params")
    private String params;

    private Integer coverSign;

    public static String getNoticeTypeName() {
        return "notice_protocol_id";
    }

    public static String getTemplateName() {
        return "template";
    }

    public static String getParamsName() {
        return "params";
    }

    public static String getCoverSignFieldName() {
        return "cover_sign";
    }

    public static String getNoticeProtocolIdFieldName() {
        return "notice_protocol_id";
    }


}
