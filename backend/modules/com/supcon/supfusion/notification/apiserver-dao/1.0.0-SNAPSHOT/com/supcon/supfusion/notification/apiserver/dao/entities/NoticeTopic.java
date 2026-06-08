package com.supcon.supfusion.notification.apiserver.dao.entities;

import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
    * 协议主题表
    * </p>
 *
 * @author panzk
 * @since 2020-07-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class NoticeTopic extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 协议主题表ID
     */
    private Long id;

    /**
     * 主题编号
     */
    private String code;

    /**
     * 主题名称
     */
    private String name;

    /**
     * 主题来源
     */
    private String source;

    /**
     * 修改标志,0 不允许修改 1 允许修改
     */
    private Integer modifySign;

    /**
     * 主题类型ID
     */
    private Long noticeTopicTypeId;

    /**
     * 主题类型描述
     */
    private String description;

    /**
     * 版本
     */
    private Integer version;

    /**
     * 是否删除
     */
    private Integer valid;

    /**
     * 主题排序字段
     */
    private Double sortValue;


    public static String getIdFieldName() {
        return "id";
    }
    public static String getCodeFieldName() {
        return "code";
    }
    public static String getNameFieldName() {
        return "name";
    }
    public static String getSourceFieldName() {
        return "source";
    }
    public static String getModifySignFieldName() {
        return "modify_sign";
    }
    public static String getNoticeTopicTypeIdFieldName() {
        return "notice_topic_type_id";
    }
    public static String getDescriptionFieldName() {
        return "description";
    }
    public static String getVersionFieldName() {
        return "version";
    }
    public static String getValidFieldName() {
        return "valid";
    }
    public static String getSortValueFieldName() {
        return "sort_value";
    }

}
