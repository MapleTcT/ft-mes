package com.supcon.supfusion.systemconfig.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.*;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@TableName("systemconfig_config_catalog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ConfigCatalogPO extends BaseEntity {
    private Long id;
    private Long parentId;
    private Double sort;
    private String code;
    private String name;
    private Boolean hasHide;
    private String appCode;
    private Integer catalogType;
}
