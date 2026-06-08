package com.supcon.supfusion.systemconfig.service.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lifangyuan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigInfoBo {
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
    private Boolean isRequire;
    private String custom;
    private String appCode;
    private String moduleCode;
    private Integer widgetType;
    private String description;
}
