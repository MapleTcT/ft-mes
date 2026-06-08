package com.supcon.supfusion.systemconfig.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@TableName("systemconfig_config_info")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConfigInfoPO extends BaseEntity {
    private Long id;
    private String code;
    private String name;
    private Double sort;
    private Long catalogId;
    private String widgetValue;
    private String defaultValue;
    private Integer maxValue;
    private Integer minValue;
    private String regFormat;
    private String regMessage;
    private Boolean hasRequire;
    private String custom;
    private String appCode;
    private String moduleCode;
    private Integer widgetType;
    private String description;
}
