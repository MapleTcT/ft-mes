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
public class CatalogBo {
    private Long id;
    private Long parentId;
    private Double sort;
    private String code;
    private String name;
    private Boolean isHide;
    private String appCode;
    private Integer catalogType;
}
