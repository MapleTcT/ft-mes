package com.supcon.supfusion.notification.admin.webapi.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NoticeProtocolTmplVO extends VO {
    /**
     * 协议基础模板ID
     */
    @JsonSerialize(using = IDJsonSerializer.class)
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
    @JsonSerialize(using = IDJsonSerializer.class)
    private Long noticeProtocolId;
}
