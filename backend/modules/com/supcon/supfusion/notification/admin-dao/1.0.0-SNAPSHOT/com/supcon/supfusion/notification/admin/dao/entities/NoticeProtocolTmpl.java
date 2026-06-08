package com.supcon.supfusion.notification.admin.dao.entities;

import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 协议基础模板表
 * </p>
 *
 * @author panzk
 * @since 2020-05-11
 */
@Data
@Accessors(chain = true)
public class NoticeProtocolTmpl extends BaseEntity {

    /**
     * 协议基础模板ID
     */
    private Long id;

    /**
     * 基础模板编号
     */
    private String code;

    /**
     * 基础模板名称
     */
    private String name;

    /**
     * 国际化key
     */
    private String i18nKey;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 基础内容模板
     */
    private String template;

    /**
     * 是否为系统基础内容模板
     */
    private Integer system;


    /**
     * 协议ID
     */
    private Long noticeProtocolId;

    public static String getIdFieldName() {
        return "id";
    }

    public static String getCodeFieldName() {
        return "code";
    }

    public static String getNameFieldName() {
        return "name";
    }

    public static String getDescriptionFieldName() {
        return "description";
    }

    public static String getTemplateFieldName() {
        return "template";
    }

    public static String getNoticeProtocolIdFieldName() {
        return "notice_protocol_id";
    }

    public static String getCreateTimeFieldName() {
        return "create_time";
    }


}
