package com.supcon.supfusion.notification.apiserver.dao.entities;

import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 协议模板表
 * </p>
 *
 * @author panzk
 * @since 2020-07-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeTmpl extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 协议模板表ID
     */
    private Long id;

    /**
     * 协议模板编号
     */
    private String code;

    /**
     * 协议模板名称
     */
    private String name;

    /**
     * 模板默认参数
     */
    private String params;

    /**
     * 协议模板描述
     */
    private String description;

    /**
     * 协议模板内容
     */
    private String template;

    /**
     * 协议模板来源
     */
    private String source;

    /**
     * 修改标志,0 不允许修改 1 允许修改
     */
    private Integer modifySign;

    /**
     * 协议表ID
     */
    private Long noticeProtocolId;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 排序
     */
    private Double sortValue;

    /**
     * 是否删除
     */
    private Integer valid;


    public static String getIdFieldName() {
        return "id";
    }

    public static String getCodeFieldName() {
        return "code";
    }

    public static String getNameFieldName() {
        return "name";
    }

    public static String getParamsFieldName() {
        return "params";
    }

    public static String getDescriptionFieldName() {
        return "description";
    }

    public static String getTemplateFieldName() {
        return "template";
    }

    public static String getSourceFieldName() {
        return "source";
    }

    public static String getModifySignFieldName() {
        return "modify_sign";
    }

    public static String getNoticeProtocolIdFieldName() {
        return "notice_protocol_id";
    }

    public static String getVersionFieldName() {
        return "version";
    }

    public static String getSortValueFieldName() {
        return "sort_value";
    }

    public static String getValidFieldName() {
        return "valid";
    }

}
