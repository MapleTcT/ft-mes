package com.supcon.supfusion.systemconfig.common.client.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author lifangyuan
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogsVo {
    private Integer type;
    private List<CatalogVo> catalogs;
}
