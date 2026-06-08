package com.supcon.supfusion.systemconfig.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogNamesVO extends VO {
    private List<CatalogNameVO> catalogs;
}
